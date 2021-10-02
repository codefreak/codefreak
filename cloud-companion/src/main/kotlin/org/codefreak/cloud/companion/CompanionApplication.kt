package org.codefreak.cloud.companion

import org.apache.tika.Tika
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
@EnableConfigurationProperties(CompanionConfig::class)
class CompanionApplication {
  @Bean
  fun tika(): Tika = Tika()
}

fun main(args: Array<String>) {
  runApplication<CompanionApplication>(*args)
}
