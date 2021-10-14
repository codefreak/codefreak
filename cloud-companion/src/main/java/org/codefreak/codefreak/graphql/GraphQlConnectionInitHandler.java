package org.codefreak.codefreak.graphql;

import java.util.Map;
import reactor.core.publisher.Mono;

public interface GraphQlConnectionInitHandler {
  Mono<Map<String, Object>> handleInit(Map<String, Object> payload)
    throws GraphQlConnectionInitException;
}
