package org.codefreak.cloud.companion.web

import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeText
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.BodyInserters

@WebFluxTest(controllers = [UploadController::class])
class UploadControllerTest : FileBasedTest() {
  @Test
  fun `can upload new files to root directory`() {
    client.post()
        .uri("/upload")
        .body(uploadFormData("file123.txt", "Hello World"))
        .exchange()
        .expectStatus()
        .isCreated
    MatcherAssert.assertThat(fileService.resolve("/file123.txt").exists(), CoreMatchers.equalTo(true))
  }

  @Test
  fun `can upload new files with whitespaces`() {
    client.post()
        .uri("/upload")
        .body(uploadFormData("file test.txt", "Hello World"))
        .exchange()
        .expectStatus()
        .isCreated
    MatcherAssert.assertThat(fileService.resolve("/file test.txt").exists(), CoreMatchers.equalTo(true))
  }

  @Test
  fun `can upload new files to non-existing sub directories`() {
    client.post()
        .uri("/upload")
        .body(uploadFormData("sub/dir/file123.txt", "Hello World"))
        .exchange()
        .expectStatus()
        .isCreated
    MatcherAssert.assertThat(fileService.resolve("/sub/dir/file123.txt").exists(), CoreMatchers.equalTo(true))
  }

  @Test
  fun `return 400 in case part of sub-directory is an existing file`() {
    fileService.resolve("/sub").writeText("Hello World")
    client.post()
        .uri("/upload")
        .body(uploadFormData("sub/dir/file123.txt", "Hello World"))
        .exchange()
        .expectStatus()
        .isBadRequest
    MatcherAssert.assertThat(fileService.resolve("/sub").isRegularFile(), CoreMatchers.equalTo(true))
  }

  @Test
  fun `return 400 if uploaded file has no filename specified`() {
    client.post()
        .uri("/upload")
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
