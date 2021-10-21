package org.codefreak.codefreak.graphql;

import java.util.Map;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public interface GraphQlConnectionInitHandler {
  Mono<Map<String, Object>> handleInit(
    Map<String, Object> payload,
    WebSocketSession webSocketSession
  ) throws GraphQlConnectionInitException;
}
