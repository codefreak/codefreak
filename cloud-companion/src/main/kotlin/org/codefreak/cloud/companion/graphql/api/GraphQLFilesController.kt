package org.codefreak.cloud.companion.graphql.api

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import org.codefreak.cloud.companion.FileService
import org.codefreak.cloud.companion.graphql.model.FileSystemEvent
import org.codefreak.cloud.companion.graphql.model.FileSystemEventType
import org.codefreak.cloud.companion.graphql.model.FileSystemNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Controller
class GraphQLFilesController {
  @Autowired
  lateinit var fileService: FileService

  @QueryMapping
  fun listFiles(@Argument path: String): Mono<List<FileSystemNode>> {
    return Mono.just(fileService.resolve(path))
      .flatMapMany {
        when {
          !it.exists() -> Flux.error(IllegalArgumentException("Directory $path does not exist"))
          !it.isDirectory() -> Flux.error(IllegalArgumentException("$path is not a directory"))
          else -> Flux.fromStream {
            Files.list(it)
          }
        }
      }
      .map {
        FileSystemNode(fileService.relativePath(it), it.toFile())
      }
      .collectList()
  }

  /**
   * Watch given directory for changes (new, deleted, modified files).
   * For new files this will trigger "new" and "modified"
   */
  @SubscriptionMapping
  fun watchFiles(@Argument path: String): Flux<FileSystemEvent> {
    val dir = fileService.resolve(path)
    return fileService.watchDirectory(dir).map {
      val eventPath = it.context() as Path
      FileSystemEvent(
        fileService.relativePath(dir.resolve(eventPath)),
        FileSystemEventType(it.kind())
      )
    }
  }
}
