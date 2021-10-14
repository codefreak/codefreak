package org.codefreak.cloud.companion.graphql

import org.codefreak.codefreak.graphql.EnhancedGraphQlWebsocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.boot.GraphQlProperties
import org.springframework.graphql.web.WebGraphQlHandler
import org.springframework.graphql.web.webflux.GraphQlWebSocketHandler
import org.springframework.http.codec.ServerCodecConfigurer

@Configuration
class GraphqlConfiguration {

  @Bean
  fun graphQlWebSocketHandler(
    webGraphQlHandler: WebGraphQlHandler,
    properties: GraphQlProperties,
    configurer: ServerCodecConfigurer
  ): GraphQlWebSocketHandler {
    return EnhancedGraphQlWebsocketHandler(
      webGraphQlHandler,
      configurer,
      properties.websocket.connectionInitTimeout,
      null
    )
  }
}
