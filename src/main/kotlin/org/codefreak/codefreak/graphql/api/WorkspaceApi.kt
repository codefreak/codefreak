package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import org.codefreak.codefreak.cloud.RemoteWorkspaceReference
import org.codefreak.codefreak.cloud.WorkspaceService
import org.codefreak.codefreak.graphql.BaseResolver
import org.springframework.beans.factory.annotation.Autowired
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

  @Autowired
  private lateinit var workspaceService: WorkspaceService

  fun startWorkspace(fileContext: FileContext): WorkspaceDto {
    val workspaceReference = workspaceService.createWorkspace(
        workspaceService.createWorkspaceConfigForCollection(fileContext.id)
    )
    return WorkspaceDto(workspaceReference)
  }
}
