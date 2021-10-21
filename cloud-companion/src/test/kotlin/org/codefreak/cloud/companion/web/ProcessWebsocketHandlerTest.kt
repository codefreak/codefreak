package org.codefreak.cloud.companion.web

import java.net.URI
import java.util.concurrent.atomic.AtomicReference
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
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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

    val closeStatus = AtomicReference<CloseStatus>()
    val subscription =
      ReactorNettyWebSocketClient().execute(URI("ws://localhost:$localServerPort/process/$testProcessId")) { session ->
        session.send(Flux.just("echo hello \$TERM\n", "exit\n").map(session::textMessage))
          .then(session.receive().doOnEach { receivedOutput.append(it.get()?.payloadAsText) }.then())
          .then(session.closeMono().map { closeStatus.set(it) })
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

  /**
   * Closes a websocket session and returns a mono with the CloseStatus
   */
  private fun WebSocketSession.closeMono(): Mono<CloseStatus> {
    return Mono.fromCallable { this.close() }.then(this.closeStatus())
  }
}
