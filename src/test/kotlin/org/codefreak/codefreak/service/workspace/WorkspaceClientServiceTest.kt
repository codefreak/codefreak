package org.codefreak.codefreak.service.workspace

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.CoreMatchers.sameInstance
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class WorkspaceClientServiceTest {
  @Mock
  private lateinit var objectMapper: ObjectMapper

  private lateinit var workspaceClientService: WorkspaceClientService

  @BeforeEach
  fun beforeEach() {
    workspaceClientService = WorkspaceClientService(objectMapper, null)
  }

  @Test
  fun `workspace clients are recycled for same remote reference`() {
    val client1 = workspaceClientService.getClient(
      RemoteWorkspaceReference(
        WorkspaceIdentifier(WorkspacePurpose.ANSWER_IDE, "ws-1"),
        "http://localhost/ws-1"
      )
    )
    val client2 = workspaceClientService.getClient(
      RemoteWorkspaceReference(
        WorkspaceIdentifier(WorkspacePurpose.ANSWER_IDE, "ws-1"),
        "http://localhost/ws-1"
      )
    )
    assertThat(client1, sameInstance(client2))
  }
}
