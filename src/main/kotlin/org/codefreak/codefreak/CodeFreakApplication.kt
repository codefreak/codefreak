package org.codefreak.codefreak

import org.codefreak.codefreak.config.AppConfiguration
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableAspectJAutoProxy
@EnableBatchProcessing
class CodeFreakApplication {

//  private val log = LoggerFactory.getLogger(this::class.java)
//
//  @Autowired
//  private lateinit var config: AppConfiguration
//
//  override fun run(vararg args: String?) {
//    log.info("Code FREAK instance id: ${config.instanceId}")
//  }
}

fun main(args: Array<String>) {
  runApplication<CodeFreakApplication>(*args)
}
