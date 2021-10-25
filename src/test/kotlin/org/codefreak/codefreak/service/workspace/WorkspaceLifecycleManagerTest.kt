package org.codefreak.codefreak.service.workspace

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.awaitility.Awaitility.await
import org.codefreak.codefreak.EXTERNAL_INTEGRATION_TEST
import org.codefreak.codefreak.service.WorkspaceBaseTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired

@Tag(EXTERNAL_INTEGRATION_TEST)
class WorkspaceLifecycleManagerTest : WorkspaceBaseTest() {
  @Mock
  private lateinit var clock: Clock

  @Autowired
  private lateinit var clientService: WorkspaceClientService

  private lateinit var lifecycleManager: WorkspaceLifecycleManager

  private val now = Instant.parse("2021-01-01T01:01:01Z")

  private val identifier = WorkspaceIdentifier(
    purpose = WorkspacePurpose.TASK_IDE,
    reference = UUID(0, 0).toString()
  )

  @BeforeEach
  fun setUp() {
    whenever(fileService.readCollectionTar(any())).thenReturn(createTarWithEntries(emptyMap()))
    whenever(fileService.writeCollectionTar(any())).thenReturn(ByteArrayOutputStream())
    workspaceService.createWorkspace(
      identifier,
      WorkspaceConfiguration(
        scripts = emptyMap(),
        imageName = appConfiguration.workspaces.companionImage
      )
    )
    lifecycleManager = WorkspaceLifecycleManager(removeTimeout = 15, workspaceService, clientService)
    lifecycleManager.clock = clock
  }

  @Test
  fun removeIdleWorkspaces() {
    // Initial cycle, nothing should happen
    whenever(clock.instant()).thenReturn(now)
    lifecycleManager.removeIdleWorkspaces()
    await().during(10, TimeUnit.SECONDS).timeout(11, TimeUnit.SECONDS).untilAsserted {
      assertThat(workspaceService.findAllWorkspaces().filter { it.identifier == identifier }, hasSize(1))
    }
    // let 10sec pass, nothing should happen
    whenever(clock.instant()).thenReturn(now.plusSeconds(10))
    lifecycleManager.removeIdleWorkspaces()
    await().during(10, TimeUnit.SECONDS).timeout(11, TimeUnit.SECONDS).untilAsserted {
      assertThat(workspaceService.findAllWorkspaces().filter { it.identifier == identifier }, hasSize(1))
    }
    // let another 10sec pass -> workspace should be removed
    whenever(clock.instant()).thenReturn(now.plusSeconds(20))
    lifecycleManager.removeIdleWorkspaces()
    // deletion will take some time
    await().atMost(20, TimeUnit.SECONDS).untilAsserted {
      assertThat(workspaceService.findAllWorkspaces().filter { it.identifier == identifier }, hasSize(0))
    }
    // make sure files have been saved
    verify(fileService, times(1)).writeCollectionTar(UUID(0, 0))
  }
}
