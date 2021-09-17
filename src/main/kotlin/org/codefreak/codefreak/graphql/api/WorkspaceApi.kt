package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import org.codefreak.codefreak.cloud.RemoteWorkspaceReference
import org.codefreak.codefreak.cloud.KubernetesWorkspaceConfig
import org.codefreak.codefreak.cloud.KubernetesWorkspaceService
import org.codefreak.codefreak.cloud.WorkspaceReference
import org.codefreak.codefreak.graphql.BaseResolver
import org.springframework.stereotype.Component

@GraphQLName("Workspace")
class WorkspaceDto(val baseUrl: String, val authToken: String) {
  constructor(reference: WorkspaceReference) : this(
      baseUrl = reference.baseUrl,
      authToken = reference.authToken
  )
}

@Component
class WorkspaceMutation : BaseResolver(), Mutation {
  fun startWorkspace(fileContext: FileContext): WorkspaceDto {
    return withWorkspaceConfig(fileContext) { workspaceService, workspaceConfig ->
      WorkspaceDto(workspaceService.createWorkspace(workspaceConfig))
    }
  }

  fun deleteWorkspace(fileContext: FileContext): Boolean {
    withWorkspaceConfig(fileContext) { workspaceService, workspaceConfig ->
      workspaceService.deleteWorkspace(workspaceConfig)
    }
    return true
  }

  private fun <T> withWorkspaceConfig(fileContext: FileContext, consume: (service: KubernetesWorkspaceService, config: KubernetesWorkspaceConfig) -> T): T = context {
    val workspaceService = serviceAccess.getService(KubernetesWorkspaceService::class)
    val workspaceConfiguration = workspaceService.createWorkspaceConfigForCollection(fileContext.id)
    consume(workspaceService, workspaceConfiguration)
  }
}
