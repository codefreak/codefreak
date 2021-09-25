package org.codefreak.cloud.companion.web

import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import org.hamcrest.MatcherAssert
import org.hamcrest.io.FileMatchers
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.web.reactive.server.expectBody

@WebFluxTest(controllers = [FilesController::class])
internal class FilesControllerTest : FileBasedTest() {

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

  @Test
  fun `touching regular file creates empty file`() {
    client.post()
        .uri("/files/foo")
        .exchange()
        .expectStatus()
        .isCreated
    MatcherAssert.assertThat(fileService.resolve("/foo").toFile(), FileMatchers.anExistingFile())
  }

  @Test
  fun `touching an existing file does nothing`() {
    fileService.resolve("/foo").createFile().writeText("foo")
    client.post()
        .uri("/files/foo")
        .exchange()
        .expectStatus()
        .isCreated
    MatcherAssert.assertThat(fileService.resolve("/foo").toFile(), FileMatchers.anExistingFile())
  }

  @Test
  fun `touching an existing directory as a file fails`() {
    fileService.resolve("/foo").createDirectory()
    client.post()
        .uri("/files/foo")
        .exchange()
        .expectStatus()
        .isBadRequest
  }

  @Test
  fun `touching a non-existing directory creates an empty directory`() {
    client.post()
        .uri("/files/foo/")
        .exchange()
        .expectStatus()
        .isCreated
    MatcherAssert.assertThat(fileService.resolve("/foo").toFile(), FileMatchers.anExistingDirectory())
  }

  @Test
  fun `touching an existing directory does nothing`() {
    fileService.resolve("/foo").createDirectory()
    client.post()
        .uri("/files/foo/")
        .exchange()
        .expectStatus()
        .isCreated
    MatcherAssert.assertThat(fileService.resolve("/foo").toFile(), FileMatchers.anExistingDirectory())
  }

  @Test
  fun `touching an existing file as a directory fails`() {
    fileService.resolve("/foo").createFile()
    client.post()
        .uri("/files/foo/")
        .exchange()
        .expectStatus()
        .isBadRequest
  }

  @Test
  fun `touching a file as a child of another file fails`() {
    fileService.resolve("/foo").createFile()
    client.post()
        .uri("/files/foo/bar")
        .exchange()
        .expectStatus()
        .isBadRequest
  }

  @Test
  fun `touching a file inside a directory works`() {
    fileService.resolve("/foo").createDirectory()
    client.post()
        .uri("/files/foo/bar")
        .exchange()
        .expectStatus()
        .isCreated
    MatcherAssert.assertThat(fileService.resolve("/foo/bar").toFile(), FileMatchers.anExistingFile())
  }
}
