package de.code_freak.codefreak

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CodeFreakApplication

fun main(args: Array<String>) {
  runApplication<CodeFreakApplication>(*args)
}
