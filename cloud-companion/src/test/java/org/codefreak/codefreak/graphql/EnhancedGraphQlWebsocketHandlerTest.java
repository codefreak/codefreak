/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codefreak.codefreak.graphql;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;

import graphql.schema.idl.TypeRuntimeWiring;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.graphql.execution.ExecutionGraphQlService;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.web.WebGraphQlHandler;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class EnhancedGraphQlWebsocketHandlerTest {

  private static final Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();

  private static final String QUERY =
    "{\"id\": \"1\", \"type\": \"subscribe\", \"payload\": { \"query\": \"{ test { id } }\"}}";

  @Test
  void unauthorizedWithoutConnectionInit() {
    TestWebSocketSession session = handle(
      Flux.just(toWebSocketMessage(QUERY)),
      null
    );

    StepVerifier.create(session.getOutput()).verifyComplete();
    StepVerifier
      .create(session.closeStatus())
      .expectNext(new CloseStatus(4401, "Unauthorized"))
      .verifyComplete();
  }

  @Test
  void ackDeclined() {
    TestWebSocketSession session = handle(
      Flux.just(toWebSocketMessage("{\"type\":\"connection_init\"}")),
      (payload, s) -> {
        throw GraphQlConnectionInitException.fromCode(4444, "Nope");
      }
    );

    StepVerifier.create(session.getOutput()).verifyComplete();

    StepVerifier
      .create(session.closeStatus())
      .expectNext(new CloseStatus(4444, "Nope"))
      .verifyComplete();
  }

  @Test
  void customInitPayloadAndResponse() {
    TestWebSocketSession session = handle(
      Flux.just(
        toWebSocketMessage(
          "{\"type\":\"connection_init\", \"payload\": {\"in\": \"foo\"}}"
        )
      ),
      (payload, s) -> {
        Map<String, Object> ret = new HashMap<>();
        ret.put("out", payload.get("in"));
        return Mono.just(ret);
      }
    );

    StepVerifier
      .create(session.getOutput())
      .consumeNextWith(message -> {
        Map<String, Object> messageContent = decode(message);
        assertThat(messageContent, hasEntry("type", "connection_ack"));
        assertThat(messageContent, hasKey("payload"));
        assertThat(
          (Map<String, Object>) messageContent.get("payload"),
          hasEntry("out", "foo")
        );
      })
      .verifyComplete();
  }

  @Test
  void tooManyConnectionInitRequests() {
    TestWebSocketSession session = handle(
      Flux.just(
        toWebSocketMessage("{\"type\":\"connection_init\"}"),
        toWebSocketMessage("{\"type\":\"connection_init\"}")
      ),
      null
    );

    StepVerifier
      .create(session.getOutput())
      .consumeNextWith(message -> assertMessageType(message, "connection_ack"))
      .verifyComplete();

    StepVerifier
      .create(session.closeStatus())
      .expectNext(new CloseStatus(4429, "Too many initialisation requests"))
      .verifyComplete();
  }

  private TestWebSocketSession handle(
    Flux<WebSocketMessage> input,
    GraphQlConnectionInitHandler initHandler
  ) {
    EnhancedGraphQlWebsocketHandler handler = new EnhancedGraphQlWebsocketHandler(
      initWebGraphQlHandler(),
      ServerCodecConfigurer.create(),
      Duration.ofSeconds(60),
      initHandler
    );

    TestWebSocketSession session = new TestWebSocketSession(input);
    handler.handle(session).block();
    return session;
  }

  private static WebSocketMessage toWebSocketMessage(String data) {
    DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(
      data.getBytes(StandardCharsets.UTF_8)
    );
    return new WebSocketMessage(WebSocketMessage.Type.TEXT, buffer);
  }

  @SuppressWarnings({ "unchecked", "ConstantConditions" })
  private Map<String, Object> decode(WebSocketMessage message) {
    return (Map<String, Object>) decoder.decode(
      DataBufferUtils.retain(message.getPayload()),
      EnhancedGraphQlWebsocketHandler.MAP_RESOLVABLE_TYPE,
      null,
      Collections.emptyMap()
    );
  }

  private void assertMessageType(WebSocketMessage message, String messageType) {
    Map<String, Object> map = decode(message);
    assertThat(map, hasEntry("type", messageType));
  }

  private static WebGraphQlHandler initWebGraphQlHandler() {
    return WebGraphQlHandler
      .builder(new ExecutionGraphQlService(graphQlSource()))
      .build();
  }

  private static class TestObj {

    public String getId() {
      return "foo";
    }
  }

  private static GraphQlSource graphQlSource() {
    return GraphQlSource
      .builder()
      .schemaResources(
        new ByteArrayResource(
          "type Test { id: String! } type Query { test: Test! }".getBytes()
        )
      )
      .configureRuntimeWiring(builder ->
        builder
          .type(
            TypeRuntimeWiring
              .newTypeWiring("Query")
              .dataFetcher("test", env -> new TestObj())
          )
          .build()
      )
      .build();
  }
}
