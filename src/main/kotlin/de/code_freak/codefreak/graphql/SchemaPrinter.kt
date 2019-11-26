package de.code_freak.codefreak.graphql

import com.expediagroup.graphql.extensions.print
import com.expediagroup.graphql.spring.GraphQLConfigurationProperties
import com.expediagroup.graphql.spring.SchemaAutoConfiguration
import graphql.schema.GraphQLSchema
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

@ImportAutoConfiguration(SchemaAutoConfiguration::class)
@ComponentScan("de.code_freak.codefreak.graphql")
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
  }
}

fun main(args: Array<String>) {
  SpringApplicationBuilder(SchemaPrinter::class.java)
      .web(WebApplicationType.NONE)
      .run(*args)
}
