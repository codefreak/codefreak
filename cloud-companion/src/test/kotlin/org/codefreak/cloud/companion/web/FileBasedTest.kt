package org.codefreak.cloud.companion.web

import kotlin.io.path.createDirectories
import org.apache.commons.io.FileUtils
import org.codefreak.cloud.companion.CompanionConfig
import org.codefreak.cloud.companion.FileService
import org.codefreak.cloud.companion.security.SecurityConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(SpringExtension::class)
@Import(FileService::class, CompanionConfig::class, SecurityConfiguration::class)
@ActiveProfiles("test")
abstract class FileBasedTest {
  @Autowired
  protected lateinit var client: WebTestClient

  @Autowired
  protected lateinit var fileService: FileService

  @BeforeEach
  fun setup() {
    fileService.resolve("/").createDirectories()
  }

  @AfterEach
  fun tearDown() {
    // ensure clean directory after each test
    FileUtils.cleanDirectory(fileService.resolve("/").toFile())
  }
}
