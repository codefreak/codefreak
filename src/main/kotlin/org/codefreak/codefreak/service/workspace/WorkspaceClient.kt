package org.codefreak.codefreak.service.workspace

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.ws.DefaultWebSocketEngine
import com.apollographql.apollo3.network.ws.GraphQLWsProtocol
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.first
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.BufferedSink
import okio.ByteString
import okio.source
import org.codefreak.codefreak.service.workspace.gql.StartProcessMutation
import org.codefreak.codefreak.service.workspace.gql.WaitForProcessSubscription
import org.codefreak.codefreak.util.preventClose
import org.slf4j.LoggerFactory
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class WorkspaceClient(
  baseUrl: String,
  private val objectMapper: ObjectMapper
) {
  companion object {
    private val log = LoggerFactory.getLogger(WorkspaceClient::class.java)
  }

  private val baseUri = URI(baseUrl)
  private val requestFactory = OkHttpClient.Builder()
    .retryOnConnectionFailure(false)
    .build()

  val apolloClient = ApolloClient(
    networkTransport = WebSocketNetworkTransport(
      serverUrl = buildWorkspaceUri(path = "/graphql"),
      protocol = GraphQLWsProtocol(),
      webSocketEngine = DefaultWebSocketEngine(
        webSocketFactory = requestFactory
      )
    )
  )

  /**
   * Starts a process in the workspace with the specified arguments.
   * Additional environment variables can be passed as KEY=VALUE string-pairs.
   */
  suspend fun startProcess(cmd: List<String>, additionalEnv: List<String>? = null): String {
    return apolloClient.mutate(StartProcessMutation(cmd, additionalEnv)).dataOrThrow.startProcess.id as String
  }

  /**
   * Wait for the specified process inside the workspace to finish and returns the given exit-code
   */
  suspend fun waitForProcess(processId: String): Int {
    return apolloClient.subscribe(WaitForProcessSubscription(processId)).first().dataOrThrow.waitForProcess
  }

  /**
   * Upload a raw tar archive to the workspace.
   * The files of this archive will be extracted to the workspace project directory and override
   * ALL existing files. Make sure the workspace's existing files have been saved properly before invoking this method.
   */
  fun deployFiles(tarArchiveStream: InputStream) {
    val body = inputStreamRequestBody(okhttp3.MediaType.get("application/x-tar"), tarArchiveStream)
    val request = Request.Builder()
      .post(body)
      .url(buildWorkspaceUri(path = "/files-tar"))
      .header("Connection", "close")
      .build()
    requestFactory.newCall(request).execute { response ->
      if (response.code() != 201) {
        throw IllegalStateException("Expected status 201 CREATED when deploying files to workspace. Received ${response.code()} instead.")
      }
    }
  }

  /**
   * Download the project files of the workspace as raw tar archive.
   * The filter parameter allows to limit the included files by a glob-pattern (AntPathMatcher).
   */
  fun <T> downloadTar(filter: String? = null, consumer: (tarStream: InputStream) -> T): T {
    val request = Request.Builder()
      .get()
      .url(buildWorkspaceUri(
        path = "/files-tar",
        query = filter?.run { "filter=$filter" }
      ))
      .header("Connection", "close")
      .build()
    requestFactory.newCall(request).execute { response ->
      val body = response.body() ?: throw IllegalStateException("Downloading files returned no body")
      return body.byteStream().use { tarStream ->
        // prevent others from closing our HTTP connection
        consumer(tarStream.preventClose())
      }
    }
  }

  /**
   * Wait for a workspace to be reachable via HTTP
   */
  fun waitForWorkspaceToComeLive(timeout: Long, unit: TimeUnit, interval: Long = 500L): Boolean {
    val latch = CountDownLatch(1)
    val thread = thread(name = "wait-workspace-${this.hashCode()}") {
      try {
        while (!isWorkspaceLive()) {
          Thread.sleep(interval)
        }
        latch.countDown()
      } catch (e: InterruptedException) {
        // okay
      }
    }
    return if (!latch.await(timeout, unit)) {
      thread.interrupt()
      false
    } else {
      true
    }
  }

  private fun isWorkspaceLive(): Boolean {
    val request = Request.Builder()
      .get()
      .url(buildWorkspaceUri(path = "/actuator/health/readiness"))
      .header("Connection", "close")
      .build()
    try {
      requestFactory.newCall(request).execute { response ->
        return when (val code = response.code()) {
          // 200 is the status we expect for a ready workspace
          200 -> true
          // 404 if the ingress has not been propagated
          // 503 if the service behind ingress is not ready
          503, 404 -> false
          else -> throw IllegalStateException("Expected a status of 404 or 503 but received $code instead")
        }
      }
    } catch (e: IOException) {
      log.debug("IOException occurred when trying to connect to workspace at $baseUri", e)
      return false
    }
  }

  /**
   * Determine the number of websocket connections to a workspace by calling the dedicated metric endpoint
   * exposed by the companion.
   */
  fun countWebsocketConnections(): Long {
    val request = Request.Builder()
      .get()
      .url(buildWorkspaceUri(path = "/actuator/metrics/http.websocket.connections"))
      .header("Connection", "close")
      .build()
    requestFactory.newCall(request).execute { response ->
      val body = response.body() ?: throw IllegalStateException("Metric response has no body")
      return when (val code = response.code()) {
        200 -> extractFirstMeasurementValue(body).toLong()
        else -> throw IllegalStateException("Expected a status of 404 or 503 but received $code instead")
      }
    }
  }

  private fun extractFirstMeasurementValue(body: ResponseBody): Double {
    try {
      val bodyString = body.string()
      // You cannot marshal to a MetricResponse object here because it is not meant for deserialization
      val objectNode = objectMapper.readTree(bodyString)
      return objectNode.get("measurements").get(0).get("value").doubleValue()
    } catch (e: NoSuchElementException) {
      throw IllegalArgumentException("Returned payload from workspace does not contain any measurements.")
    } catch (e: IllegalArgumentException) {
      throw IllegalArgumentException("Returned payload from workspace is no valid JSON.")
    }
  }

  /**
   * Collect the full process output as string. Keep in mind the mono will complete only once
   * the process has finished.
   */
  fun getAllProcessOutput(processId: String): Mono<String> {
    return getProcessOutput(processId).collect(Collectors.joining())
  }

  /**
   * Stream the process output as it comes in
   */
  fun getProcessOutput(processId: String): Flux<String> {
    val request = Request.Builder()
      .get()
      .url(buildWorkspaceUri(path = "/process/$processId", websocket = true))
      .build()
    return Flux.create { sink ->
      requestFactory.newWebSocket(request, object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
          sink.onDispose {
            // https://datatracker.ietf.org/doc/html/rfc6455#section-7.4
            // 1001 indicates that an endpoint is "going away", such as a server
            //      going down or a browser having navigated away from a page.
            webSocket.close(1001, null)
          }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
          sink.next(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
          onMessage(webSocket, bytes.utf8())
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
          sink.complete()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
          sink.error(t)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
          webSocket.close(1000, null)
          onClosed(webSocket, code, reason)
        }
      })
    }
  }

  private fun inputStreamRequestBody(mediaType: okhttp3.MediaType, inputStream: InputStream): RequestBody {
    return object : RequestBody() {
      override fun contentType() = mediaType
      override fun writeTo(sink: BufferedSink) {
        // do not close source or InputStream here
        sink.writeAll(inputStream.source())
      }
    }
  }

  /**
   * Automatically closes the body after consuming the response
   * TODO: This is only working when "Connection close" header is sent.
   */
  private inline fun <T> Call.execute(responseConsumer: (response: Response) -> T): T = execute().use(responseConsumer)

  /**
   * Allows building specific workspace urls e.g. for the GraphQL endpoint
   */
  private fun buildWorkspaceUri(path: String, query: String? = null, websocket: Boolean = false): String {
    val scheme = if (websocket) {
      if (baseUri.scheme == "https") "wss" else "ws"
    } else {
      baseUri.scheme
    }
    return UriComponentsBuilder.fromUri(baseUri)
      .scheme(scheme)
      // this will encode each part of the path correctly but requires us to split the
      // path into its individual parts
      .pathSegment(*path.split('/').toTypedArray())
      .query(query)
      .build()
      .encode()
      .toUriString()
  }
}
