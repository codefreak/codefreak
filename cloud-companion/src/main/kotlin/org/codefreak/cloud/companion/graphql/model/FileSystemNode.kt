package org.codefreak.cloud.companion.graphql.model

import org.springframework.graphql.boot.GraphQlSourceBuilderCustomizer
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.stereotype.Component

/**
 * Factory for new instances of Directory/File depending on the type of file
 */
fun FileSystemNode(relativePath: String, file: java.io.File): FileSystemNode {
  return if (file.isDirectory) {
    Directory(relativePath)
  } else {
    File(relativePath, file)
  }
}

interface FileSystemNode {
  val path: String
}

@Component
class FileSystemNodeResolver : GraphQlSourceBuilderCustomizer {
  override fun customize(builder: GraphQlSource.Builder) {
    builder.configureRuntimeWiring {
      it.type("FileSystemNode") { wiring ->
        wiring.typeResolver {
          when (it.getObject<FileSystemNode>()) {
            is Directory -> it.schema.getObjectType("Directory")
            else -> it.schema.getObjectType("File")
          }
        }
      }
    }
  }
}
