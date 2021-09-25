package org.codefreak.cloud.companion.web

import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.web.reactive.server.expectBody

@WebFluxTest(controllers = [FilesController::class])
internal class GraphQLFilesControllerTest : FileBasedTest() {

  @Test
  fun `can download existing file properly`() {
    fileService.resolve("/test").writeText("Hello World")
    client.get()
        .uri("/files/test")
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/plain;charset=utf-8")
        .expectBody<String>()
        .isEqualTo("Hello World")
  }

  @Test
  fun `can download existing files with whitespaces properly`() {
    fileService.resolve("/test space.txt").writeText("Hello World")
    client.get()
        .uri("/files/test space.txt")
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/plain;charset=utf-8")
        .expectBody<String>()
        .isEqualTo("Hello World")
  }

  @Test
  fun `serves images with image mime type`() {
    // jpeg signature ("magic number")
    fileService.resolve("/file.jpg").writeBytes("FFD8FFDB".chunked(2).map { it.toInt(16).toByte() }.toByteArray())
    client.get()
        .uri("/files/file.jpg")
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("image/jpeg")
  }

  @Test
  fun `serves all other file types with octet stream mime type`() {
    // pdf signature ("magic number")
    fileService.resolve("/file.pdf").writeBytes("255044462D".chunked(2).map { it.toInt(16).toByte() }.toByteArray())
    client.get()
        .uri("/files/file.pdf")
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("application/octet-stream")
  }

  @Test
  fun `returns 404 for non-existing files`() {
    client.get()
        .uri("/files/foo/bar/baz")
        .exchange()
        .expectStatus()
        .isNotFound
  }
}
