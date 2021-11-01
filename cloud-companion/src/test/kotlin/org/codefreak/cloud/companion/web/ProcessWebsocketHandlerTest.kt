package org.codefreak.cloud.companion.web

import org.awaitility.Awaitility.await
import org.awaitility.Durations
import org.codefreak.cloud.companion.ProcessManager
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.net.URI
import java.util.concurrent.CountDownLatch

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
internal class ProcessWebsocketHandlerTest {
  @Autowired
  private lateinit var processManager: ProcessManager

  @LocalServerPort
  private var localServerPort: Int = -1

  @Test
  fun `that process IO works as expected`() {
    val testProcessId = processManager.createProcess(listOf("/bin/bash", "-i"), mapOf("TERM" to "term"))
    val receivedOutput = StringBuilder()

    val outputReceivedLatch = CountDownLatch(1)
    val uri = URI("ws://localhost:$localServerPort/process/$testProcessId")
    val subscription = ReactorNettyWebSocketClient().execute(uri) { session ->
        Flux.zip(
          // store incoming messages in a string. Use .doOnEach() and not .map() because it's a side effect
          session.receive().log().doOnEach {
            // notify that some output has been received
            outputReceivedLatch.countDown()
            receivedOutput.append(it.get()?.payloadAsText)
          }
            // schedule receive and send on different threads or there will be a deadlock
            .subscribeOn(Schedulers.newSingle("ws-receive")),
          // the sender flux never completes on purpose. Otherwise, the WS connection will be closed (?)
          session.send(Flux.create {
            // wait until stdout has emitted something so we can be sure the console is open
            outputReceivedLatch.await()
            it.next(session.textMessage("echo hello \$TERM\nexit\n"))
          }).log()
            // schedule receive and send on different threads or there will be a deadlock
            .subscribeOn(Schedulers.newSingle("ws-send"))
        )
          .then()
      }.subscribe()
    try {
      await().atMost(Durations.TEN_SECONDS).untilAsserted {
        MatcherAssert.assertThat(receivedOutput.toString(), containsString("hello term"))
      }
    } finally {
      subscription.dispose()
    }
  }
}
