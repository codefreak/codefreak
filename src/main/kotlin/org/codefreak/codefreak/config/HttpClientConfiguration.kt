package org.codefreak.codefreak.config

import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
class HttpClientConfiguration {
  @Bean
  fun httpClient(): HttpClient {
    return HttpClientBuilder.create()
        .useSystemProperties()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setSocketTimeout(30000)
                .setConnectTimeout(30000)
                .setConnectionRequestTimeout(30000)
                .build()
        ).build()
  }

  @Bean
  fun restClient(httpClient: HttpClient): RestTemplate {
    return RestTemplate(
        HttpComponentsClientHttpRequestFactory(httpClient)
    )
  }
}
