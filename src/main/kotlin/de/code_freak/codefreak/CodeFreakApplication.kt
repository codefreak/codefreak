package de.code_freak.codefreak

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ComponentScan("de.code_freak.codefreak", "asset.pipeline.springboot")
@EnableScheduling
class CodeFreakApplication : CommandLineRunner {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Value("\${code-freak.instance}")
  private lateinit var instanceId: String

  override fun run(vararg args: String?) {
    log.info("Code FREAK instance id: $instanceId")
  }
}

fun main(args: Array<String>) {
  runApplication<CodeFreakApplication>(*args)
}
