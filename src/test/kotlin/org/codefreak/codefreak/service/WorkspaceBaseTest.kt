package org.codefreak.codefreak.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import java.io.ByteArrayOutputStream
import java.io.InputStream
import org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM
import org.codefreak.codefreak.EXTERNAL_INTEGRATION_TEST
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.config.KubernetesConfiguration
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.service.workspace.KubernetesWorkspaceService
import org.codefreak.codefreak.util.TarUtil
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

/**
 * For performance reasons all tests are run in the same workspace.
 * This is why this test is annotated with [Lifecycle.PER_CLASS].
 * We need an actual web environment so the companion pods will be able to reach
 * the JWK over HTTP.
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@DirtiesContext
@ActiveProfiles("test")
@Import(KubernetesConfiguration::class, AppConfiguration::class)
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@Tag(EXTERNAL_INTEGRATION_TEST)
abstract class WorkspaceBaseTest {

  @Autowired
  protected lateinit var workspaceService: KubernetesWorkspaceService

  @MockBean
  protected lateinit var fileService: FileService

  @Autowired
  protected lateinit var appConfiguration: AppConfiguration

  protected fun createTarWithEntries(entries: Map<String, String>): InputStream {
    val tarOutput = ByteArrayOutputStream()
    TarUtil.PosixTarArchiveOutputStream(tarOutput).use {
      entries.forEach { (name, content) ->
        TarUtil.writeFileWithContent(name, content.byteInputStream(), it)
      }
      it.finish()
    }
    return tarOutput.toByteArray().inputStream()
  }

  @AfterAll
  fun cleanupWorkspaces() {
    workspaceService.findAllWorkspaces().forEach {
      // If this is missing a possible save-back to database will throw
      whenever(fileService.writeCollectionTar(any())).thenReturn(NULL_OUTPUT_STREAM)
      workspaceService.deleteWorkspace(it.identifier)
    }
  }
}
