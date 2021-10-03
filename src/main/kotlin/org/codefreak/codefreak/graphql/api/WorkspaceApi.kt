package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import java.util.UUID
import org.codefreak.codefreak.cloud.DefaultWorkspaceConfiguration
import org.codefreak.codefreak.cloud.KubernetesWorkspaceService
import org.codefreak.codefreak.cloud.RemoteWorkspaceReference
import org.codefreak.codefreak.cloud.WorkspaceIdentifier
import org.codefreak.codefreak.cloud.WorkspacePurpose
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.graphql.BaseResolver
import org.springframework.stereotype.Component

@GraphQLName("Workspace")
class WorkspaceDto(val baseUrl: String, val authToken: String) {
  constructor(reference: RemoteWorkspaceReference) : this(
    baseUrl = reference.baseUrl,
    authToken = reference.authToken
  )
}

@Component
class WorkspaceMutation : BaseResolver(), Mutation {
  fun startWorkspace(fileContext: FileContext): WorkspaceDto {
    return withWorkspaceIdentifier(fileContext) { workspaceService, identifier ->
      WorkspaceDto(workspaceService.createWorkspace(identifier, createWorkspaceConfigForCollection(fileContext.id)))
    }
  }

  fun deleteWorkspace(fileContext: FileContext): Boolean {
    withWorkspaceIdentifier(fileContext) { workspaceService, identifier ->
      workspaceService.deleteWorkspace(identifier)
    }
    return true
  }

  private fun <T> withWorkspaceIdentifier(
    fileContext: FileContext,
    consume: (service: KubernetesWorkspaceService, identifier: WorkspaceIdentifier) -> T
  ): T = context {
    val workspaceService = serviceAccess.getService(KubernetesWorkspaceService::class)
    val workspaceIdentifier = createWorkspaceIdentifier(fileContext)
    consume(workspaceService, workspaceIdentifier)
  }

  private fun createWorkspaceIdentifier(fileContext: FileContext): WorkspaceIdentifier {
    return WorkspaceIdentifier(
      purpose = mapFileContextToWorkspacePurpose(fileContext),
      reference = fileContext.id.toString()
    )
  }

  private fun mapFileContextToWorkspacePurpose(fileContext: FileContext) = when (fileContext.type) {
    FileContextType.ANSWER -> WorkspacePurpose.ANSWER_IDE
    FileContextType.TASK -> WorkspacePurpose.TASK_IDE
  }

  private fun createWorkspaceConfigForCollection(collectionId: UUID): DefaultWorkspaceConfiguration = context {
    DefaultWorkspaceConfiguration(
      reference = collectionId,
      collectionId = collectionId,
      isReadOnly = false,
      imageName = serviceAccess.getService(AppConfiguration::class).workspaces.companionImage
    )
  }
}
