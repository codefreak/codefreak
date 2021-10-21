package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.workspace.WorkspaceIdeService
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component

@GraphQLName("Workspace")
class WorkspaceDto(val baseUrl: String, val authToken: String?) {
  constructor(authedReference: WorkspaceIdeService.AuthenticatedWorkspaceReference) : this(
    baseUrl = authedReference.remoteReference.baseUrl,
    authToken = authedReference.authToken
  )
}

@Component
class WorkspaceMutation : BaseResolver(), Mutation {

  @Secured(Authority.ROLE_STUDENT)
  fun startWorkspace(fileContext: FileContext): WorkspaceDto = context {
    val answer = resolveFileContext(fileContext)
    val workspaceReference = serviceAccess.getService(WorkspaceIdeService::class)
      .createAnswerIdeForUser(answer, this.authorization.currentUser)
    WorkspaceDto(workspaceReference)
  }

  @Secured(Authority.ROLE_STUDENT)
  fun deleteWorkspace(fileContext: FileContext): Boolean = context {
    val answer = resolveFileContext(fileContext)
    serviceAccess.getService(WorkspaceIdeService::class).deleteAnswerIde(answer.id)
    true
  }

  /**
   * TODO: This won't work with fileContext other than Answer
   */
  private fun resolveFileContext(fileContext: FileContext): Answer = context {
    if (fileContext.type != FileContextType.ANSWER) {
      throw IllegalArgumentException("Only workspaces for answers are supported, currently")
    }
    val answer = serviceAccess.getService(AnswerService::class).findAnswer(fileContext.id)
    authorization.requireIsCurrentUser(answer.submission.user)
    answer
  }
}
