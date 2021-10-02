package org.codefreak.cloud.companion.web

import java.util.UUID
import org.apache.commons.io.IOUtils
import org.codefreak.cloud.companion.ProcessManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriTemplate
import reactor.core.publisher.Mono

@Component
class ProcessWebsocketHandler : WebSocketHandler {

  @Autowired
  private lateinit var processManager: ProcessManager

  private val uriTemplate = UriTemplate("/process/{id}")

  override fun handle(session: WebSocketSession): Mono<Void> {
    val processId = getProcessIdFromSession(session)

    // handle incoming messages and redirect them to process stdin
    val processInput = Mono.fromCallable {
      processManager.getStdin(processId)
    }.flatMapMany { stdinOutputStream ->
      session.receive().map {
        IOUtils.copy(it.payload.asInputStream(), stdinOutputStream)
      }
    }.then()

    // send process output
    val processOutput = session.send(
      processManager.getStdout(processId).map {
        WebSocketMessage(WebSocketMessage.Type.BINARY, it)
      }.doOnTerminate {
        // close the connection when stdout closes
        // we do not have to do this for stdin because stdin and stdout will (hopefully) close simultaneously
        session.close(CloseStatus.NORMAL)
      }
    )

    // combine both reactive streams into a single one
    return Mono.zip(processInput, processOutput).then()
  }

  private fun getProcessIdFromSession(session: WebSocketSession): UUID {
    val path = session.handshakeInfo.uri.path
    return UUID.fromString(
      uriTemplate.match(path)["id"] ?: throw RuntimeException("Invalid process specified in URI")
    )
  }
}
