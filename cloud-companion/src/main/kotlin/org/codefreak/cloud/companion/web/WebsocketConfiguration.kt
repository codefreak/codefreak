package org.codefreak.cloud.companion.web

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping

@Configuration
class WebsocketConfiguration {

  @Bean
  fun handlerMapping(handler: ProcessWebsocketHandler): HandlerMapping {
    val handlerMap: Map<String, ProcessWebsocketHandler> = mapOf(
      "/process/*" to handler
    )
    return SimpleUrlHandlerMapping(handlerMap, 1)
  }
}
