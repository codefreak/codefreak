package org.codefreak.codefreak.service.workspace

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.runBlocking
import liquibase.util.StreamUtil
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.awaitility.Awaitility.await
import org.codefreak.codefreak.EXTERNAL_INTEGRATION_TEST
import org.codefreak.codefreak.service.WorkspaceBaseTest
import org.codefreak.codefreak.util.TarUtil.entrySequence
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.aMapWithSize
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.hasEntry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.mock.mockito.MockBean

@Tag(EXTERNAL_INTEGRATION_TEST)
class WorkspaceClientTest : WorkspaceBaseTest() {
  private lateinit var remoteWorkspaceReference: RemoteWorkspaceReference

  private lateinit var workspaceClient: WorkspaceClient

  private val collectionId = UUID(0, 0)
  private val workspaceIdentifier = WorkspaceIdentifier(WorkspacePurpose.EVALUATION, collectionId.toString())

  @MockBean
  private lateinit var workspaceFileService: WorkspaceFileService

  @BeforeAll
  fun beforeAll() {
    val tar = createTarWithEntries(mapOf("file.txt" to "foo"))
    whenever(workspaceFileService.loadFiles(workspaceIdentifier, any())).then {
      it.getArgument<Consumer<InputStream>>(1).accept(tar)
    }
    remoteWorkspaceReference = workspaceService.createWorkspace(
      workspaceIdentifier,
      WorkspaceConfiguration(
        scripts = emptyMap(),
        imageName = appConfiguration.workspaces.companionImage
      )
    )
  }

  @BeforeEach
  fun setupWorkspaceClient() {
    workspaceClient = createClient()
  }

  private fun createClient() =
    WorkspaceClient(remoteWorkspaceReference.baseUrl, null, ObjectMapper().registerKotlinModule())

  @AfterEach
  fun closeConnection() {
    workspaceClient.apolloClient.dispose()
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

  @Test
  fun countWebsocketConnections() = runBlocking {
    // make sure the client is actually connected via websocket
    workspaceClient.startProcess(listOf("/bin/bash", "-i"))
    await().atMost(10, TimeUnit.SECONDS).untilAsserted {
      Assertions.assertEquals(1, workspaceClient.countWebsocketConnections())
    }

    val secondClient = createClient()
    secondClient.startProcess(listOf("/bin/bash", "-i"))
    await().atMost(10, TimeUnit.SECONDS).untilAsserted {
      Assertions.assertEquals(2, workspaceClient.countWebsocketConnections())
    }
  }
}
