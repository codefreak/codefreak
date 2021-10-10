package org.codefreak.cloud.companion.web

import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import org.apache.commons.io.FileUtils
import org.codefreak.cloud.companion.CompanionConfig
import org.codefreak.cloud.companion.FileService
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

@ExtendWith(SpringExtension::class)
@WebFluxTest(controllers = [FileController::class])
@Import(FileService::class, CompanionConfig::class, SecurityConfiguration::class)
@ActiveProfiles("test")
internal class FileControllerTest {
  @Autowired
  lateinit var client: WebTestClient

  @Autowired
  lateinit var fileService: FileService

  @BeforeEach
  fun setup() {
    fileService.resolve("/").createDirectories()
  }

  @AfterEach
  fun tearDown() {
    // ensure clean directory after each test
    FileUtils.cleanDirectory(fileService.resolve("/").toFile())
  }

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
  fun `can upload new files to root directory`() {
    client.post()
      .uri("/files")
      .body(uploadFormData("file123.txt", "Hello World"))
      .exchange()
      .expectStatus()
      .isCreated
    assertThat(fileService.resolve("/file123.txt").exists(), equalTo(true))
  }

  @Test
  fun `can upload new files with whitespaces`() {
    client.post()
      .uri("/files")
      .body(uploadFormData("file test.txt", "Hello World"))
      .exchange()
      .expectStatus()
      .isCreated
    assertThat(fileService.resolve("/file test.txt").exists(), equalTo(true))
  }

  @Test
  fun `can upload new files to non-existing sub directories`() {
    client.post()
      .uri("/files")
      .body(uploadFormData("sub/dir/file123.txt", "Hello World"))
      .exchange()
      .expectStatus()
      .isCreated
    assertThat(fileService.resolve("/sub/dir/file123.txt").exists(), equalTo(true))
  }

  @Test
  fun `return 400 in case part of sub-directory is an existing file`() {
    fileService.resolve("/sub").writeText("Hello World")
    client.post()
      .uri("/files")
      .body(uploadFormData("sub/dir/file123.txt", "Hello World"))
      .exchange()
      .expectStatus()
      .isBadRequest
    assertThat(fileService.resolve("/sub").isRegularFile(), equalTo(true))
  }

  @Test
  fun `return 400 if uploaded file has no filename specified`() {
    client.post()
      .uri("/files")
      .body(uploadFormData(null, "Hello World"))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  private fun uploadFormData(fileName: String?, content: String): BodyInserters.MultipartInserter {
    return BodyInserters.fromMultipartData(
      MultipartBodyBuilder()
        .apply {
          part(
            "files",
            ByteArrayResource(content.encodeToByteArray()),
            MediaType.TEXT_PLAIN
          ).also {
            if (fileName != null) {
              it.filename(fileName)
            }
          }
        }
        .build()
    )
  }
}
