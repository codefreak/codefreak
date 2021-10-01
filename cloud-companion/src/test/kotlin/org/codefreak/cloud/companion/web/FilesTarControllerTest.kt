package org.codefreak.cloud.companion.web

import java.io.ByteArrayOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.codefreak.cloud.companion.CompanionConfig
import org.codefreak.cloud.companion.FileService
import org.codefreak.cloud.companion.PosixTarArchiveOutputStream
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

@ExtendWith(SpringExtension::class)
@WebFluxTest(controllers = [FilesTarController::class])
@Import(FileService::class, CompanionConfig::class)
@ActiveProfiles("test")
internal class FilesTarControllerTest {
  @Autowired
  lateinit var client: WebTestClient

  @Autowired
  lateinit var fileService: FileService

  @BeforeEach
  fun setup() {
    fileService.resolve("/").createDirectories()
    FileUtils.cleanDirectory(fileService.resolve("/").toFile())
  }

  @Test
  fun testDownload() {
    fileService.resolve("/test.txt").writeText("Hello World")
    fileService.withParentPathExists("/sub-dir/file.txt") { it.writeText("Hello World") }
    val body = client.get()
      .uri("/files-tar")
      .accept(MediaType.parseMediaType("application/x-tar"))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<ByteArray>()
      .returnResult()
    body.responseBody?.let {
      val archive = TarArchiveInputStream(it.inputStream())
      val entries = generateSequence { archive.nextTarEntry }.toList()
      assertThat(entries, Matchers.hasSize(3))
      assertThat(
        entries, Matchers.containsInAnyOrder(
          Matchers.hasProperty("name", equalTo("sub-dir/")),
          Matchers.hasProperty("name", equalTo("sub-dir/file.txt")),
          Matchers.hasProperty("name", equalTo("test.txt"))
        )
      )
    } ?: fail { "No content returned from server" }
  }

  @Test
  fun testDownloadWithFileFilter() {
    fileService.resolve("/test.txt").writeText("Hello World")
    fileService.withParentPathExists("/sub-dir/file.txt") { it.writeText("Hello World") }
    val body = client.get()
      .uri("/files-tar?filter=test.txt")
      .accept(MediaType.parseMediaType("application/x-tar"))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<ByteArray>()
      .returnResult()
    body.responseBody?.let {
      val archive = TarArchiveInputStream(it.inputStream())
      val entries = generateSequence { archive.nextTarEntry }.toList()
      assertThat(entries, Matchers.hasSize(1))
      assertThat(
        entries, Matchers.containsInAnyOrder(
          Matchers.hasProperty("name", equalTo("test.txt"))
        )
      )
    } ?: fail { "No content returned from server" }
  }

  @Test
  fun testDownloadPatternFilter() {
    fileService.resolve("/test.txt").writeText("Hello World")
    fileService.withParentPathExists("/sub-dir/file.txt") { it.writeText("Hello World") }
    val body = client.get()
      .uri("/files-tar?filter=**/*.txt")
      .accept(MediaType.parseMediaType("application/x-tar"))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<ByteArray>()
      .returnResult()
    body.responseBody?.let {
      val archive = TarArchiveInputStream(it.inputStream())
      val entries = generateSequence { archive.nextTarEntry }.toList()
      assertThat(entries, Matchers.hasSize(2))
      assertThat(
        entries, Matchers.containsInAnyOrder(
          Matchers.hasProperty("name", equalTo("sub-dir/file.txt")),
          Matchers.hasProperty("name", equalTo("test.txt"))
        )
      )
    } ?: fail { "No content returned from server" }
  }

  @Test
  fun testUpload() {
    client.post()
      .uri("/files-tar")
      .contentType(MediaType.parseMediaType("application/x-tar"))
      .body(
        BodyInserters.fromResource(
          InputStreamResource(
            createArchive(
              mapOf(
                "file.txt" to "hello world",
                "sub-dir/file.txt" to "hello world 2"
              )
            ).inputStream()
          )
        )
      )
      .exchange()
      .expectStatus()
      .isCreated
    assertThat(fileService.resolve("/file.txt").exists(), equalTo(true))
    assertThat(fileService.resolve("/file.txt").readText(), equalTo("hello world"))
    assertThat(fileService.resolve("/sub-dir/file.txt").exists(), equalTo(true))
    assertThat(fileService.resolve("/sub-dir/file.txt").readText(), equalTo("hello world 2"))
  }

  private fun createArchive(entries: Map<String, String>): ByteArray {
    val output = ByteArrayOutputStream()
    val archive = PosixTarArchiveOutputStream(output)
    entries.forEach { (path, content) ->
      archive.putArchiveEntry(TarArchiveEntry(path).also {
        it.size = content.length.toLong()
      })
      IOUtils.copy(content.byteInputStream(), archive)
      archive.closeArchiveEntry()
    }
    archive.finish()
    return output.toByteArray()
  }
}
