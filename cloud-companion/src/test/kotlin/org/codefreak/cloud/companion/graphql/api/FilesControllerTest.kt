package org.codefreak.cloud.companion.graphql.api

import java.time.Duration
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import org.codefreak.cloud.companion.FileService
import org.codefreak.cloud.companion.graphql.BasicGraphqlTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.test.StepVerifier

internal class FilesControllerTest(
  @Autowired private val fileService: FileService
) : BasicGraphqlTest() {

  @BeforeEach
  fun setup() {
    fileService.resolve("/").createDirectories()
    fileService.purgeFiles()
  }

  @Test
  fun `list files delivers files`() {
    fileService.resolve("/test").writeText("Hello World")
    fileService.resolve("/test2").writeText("Hello World")
    graphQlTester.query("{ listFiles(path: \"/\"){ __typename, path } }")
      .execute()
      .path("listFiles..path")
      .entityList(String::class.java)
      .contains("/test", "/test2")
  }

  @Test
  fun `modifying files notifies correctly`() {
    val modifiedFiles = graphQlTester.query("subscription { watchFiles(path: \"/\"){ path, type } }")
      .executeSubscription()
      .toFlux()

    StepVerifier.create(modifiedFiles)
      .then { fileService.resolve("/test").createFile() }
      .consumeNextWith { it.path("watchFiles.type").matchesJson("\"CREATED\"") }
      .then { fileService.resolve("/test").writeText("foo") }
      .consumeNextWith { it.path("watchFiles.type").matchesJson("\"MODIFIED\"") }
      .thenCancel()
      .verify(Duration.ofSeconds(60))
  }
}
