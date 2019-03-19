package de.code_freak.codefreak

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan("de.code_freak.codefreak", "asset.pipeline.springboot")
class CodeFreakApplication

fun main(args: Array<String>) {
  runApplication<CodeFreakApplication>(*args)
}
