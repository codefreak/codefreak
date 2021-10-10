package org.codefreak.codefreak.service.workspace

import com.nhaarman.mockitokotlin2.whenever
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.runBlocking
import liquibase.util.StreamUtil
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.awaitility.Awaitility.await
import org.codefreak.codefreak.service.WorkspaceBaseTest
import org.codefreak.codefreak.util.TarUtil.entrySequence
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.aMapWithSize
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.hasEntry
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.springframework.beans.factory.annotation.Autowired

// Can be enabled once the Image building works on GitHub Actions
@DisabledOnOs(OS.WINDOWS)
class WorkspaceClientTest : WorkspaceBaseTest() {
  @Autowired
  private lateinit var workspaceClientService: WorkspaceClientService

  @Autowired
  private lateinit var workspaceService: KubernetesWorkspaceService

  private lateinit var workspaceClient: WorkspaceClient

  private val workspaceIdentifier = WorkspaceIdentifier(WorkspacePurpose.EVALUATION, "test")

  @BeforeAll
  fun beforeAll() {
    val collectionId = UUID(0, 0)
    val tar = createTarWithEntries(mapOf("file.txt" to "foo"))
    whenever(fileService.readCollectionTar(collectionId)).thenReturn(tar)
    val remoteWorkspaceReference = workspaceService.createWorkspace(
      workspaceIdentifier,
      WorkspaceConfiguration(
          collectionId = collectionId,
          isReadOnly = true,
          scripts = emptyMap(),
          imageName = appConfiguration.workspaces.companionImage
      )
    )
    workspaceClient = workspaceClientService.createClient(remoteWorkspaceReference)
  }

  @Test
  fun workspaceComesLive() {
    Assertions.assertTrue(workspaceClient.waitForWorkspaceToComeLive(20, TimeUnit.SECONDS))
  }

  @Test
  fun processLifecycle(): Unit = runBlocking {
    val processId =
      workspaceClient.startProcess(listOf("/bin/bash", "-c", "echo foo is not \$FOO && exit 12"), listOf("FOO=bar"))
    Assertions.assertEquals("foo is not bar", workspaceClient.getAllProcessOutput(processId).awaitLast().trim())
    Assertions.assertEquals(12, workspaceClient.waitForProcess(processId))
  }

  @Test
  fun fileUploadAndDownload() {
    // deploy a tar archive file a single file named other.txt
    val testContent = "I am a Teapot"
    val tarArchive = createTarWithEntries(mapOf("other.txt" to testContent))
    workspaceClient.deployFiles(tarArchive)
    val downloadedFiles = workspaceClient.downloadTar {
      TarArchiveInputStream(it).use { tarArchive ->
        tarArchive.entrySequence().map { archiveEntry ->
          Pair(archiveEntry.name, StreamUtil.readStreamAsString(tarArchive))
        }.toMap()
      }
    }
    assertThat(
      downloadedFiles, allOf(
        aMapWithSize(1),
        hasEntry("other.txt", testContent)
      )
    )
  }

  /**
   * This test should run last because it kills the websocket client
   */
  @Test
  @Order(Int.MAX_VALUE)
  fun countWebsocketConnections() {
    // make sure the client is actually connected
    runBlocking { workspaceClient.startProcess(listOf("/bin/bash", "-i")) }
    Assertions.assertEquals(1, workspaceClient.countWebsocketConnections())

    // disconnect and wait if the connection
    workspaceClient.apolloClient.dispose()
    await().atMost(10, TimeUnit.SECONDS).untilAsserted {
      Assertions.assertEquals(0, workspaceClient.countWebsocketConnections())
    }
  }
}
