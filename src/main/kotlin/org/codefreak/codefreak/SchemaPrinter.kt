package org.codefreak.codefreak

import com.expediagroup.graphql.extensions.print
import com.expediagroup.graphql.spring.GraphQLAutoConfiguration
import com.expediagroup.graphql.spring.GraphQLConfigurationProperties
import graphql.schema.GraphQLSchema
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.system.exitProcess
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan

@ImportAutoConfiguration(classes = [GraphQLAutoConfiguration::class, JacksonAutoConfiguration::class])
@ComponentScan("org.codefreak.codefreak")
@EnableConfigurationProperties(GraphQLConfigurationProperties::class)
class SchemaPrinter : CommandLineRunner {

  @Autowired
  private lateinit var schema: GraphQLSchema

  override fun run(vararg args: String?) {
    require(args.isNotEmpty() && args[0] != null) { "Missing path!" }
    val file = File(args[0]!!)
    println("\nWriting schema to ${file.absolutePath}")
    file.parentFile.mkdirs()
    BufferedWriter(FileWriter(file)).use {
      it.write(schema.print())
    }
    exitProcess(0)
  }
}

fun main(args: Array<String>) {
  SpringApplicationBuilder(SchemaPrinter::class.java)
      .run(*args)
}
