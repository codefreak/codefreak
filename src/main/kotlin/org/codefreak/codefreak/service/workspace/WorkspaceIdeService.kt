package org.codefreak.codefreak.service.workspace

import java.util.UUID
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Service responsible for managing IDEs based on workspaces and
 * syncing files between the database and the IDEs.
 */
@Service
class WorkspaceIdeService(
  private val workspaceService: WorkspaceService,
  @Autowired(required = false)
  private val workspaceAuthService: WorkspaceAuthService?,
  @Value("#{@config.workspaces.companionImage}")
  private val defaultWorkspaceImage: String
) {

  data class AuthenticatedWorkspaceReference(
    val remoteReference: RemoteWorkspaceReference,
    val authToken: String?
  )

  /**
   * Create an answer IDE for the given user. The returned reference will contain
   * a proper authentication token for the user.
   */
  fun createAnswerIdeForUser(answer: Answer, user: User): AuthenticatedWorkspaceReference {
    val identifier = createAnswerIdeWorkspaceIdentifier(answer.id)
    val config = createAnswerIdeWorkspaceConfig()
    val remoteReference = workspaceService.createWorkspace(identifier, config)
    return AuthenticatedWorkspaceReference(remoteReference, workspaceAuthService?.createUserAuthToken(identifier, user))
  }

  /**
   * Create a new IDE for the given answer.
   */
  fun createAnswerIde(answer: Answer): RemoteWorkspaceReference {
    val identifier = createAnswerIdeWorkspaceIdentifier(answer.id)
    val config = createAnswerIdeWorkspaceConfig()
    return workspaceService.createWorkspace(identifier, config)
  }

  /**
   * Store the current file state of the IDE back to the database.
   */
  fun saveAnswerFiles(answerId: UUID) {
    val identifier = createAnswerIdeWorkspaceIdentifier(answerId)
    workspaceService.saveWorkspaceFiles(identifier)
  }

  /**
   * Update the files in the workspace with the latest state from the collection.
   */
  fun redeployAnswerFiles(answerId: UUID) {
    val identifier = createAnswerIdeWorkspaceIdentifier(answerId)
    workspaceService.redeployWorkspaceFiles(identifier)
  }

  /**
   * Delete the IDE workspace for the given answer ID.
   * This will do nothing in case there is no matching workspace.
   */
  fun deleteAnswerIde(answerId: UUID) {
    val identifier = createAnswerIdeWorkspaceIdentifier(answerId)
    workspaceService.deleteWorkspace(identifier)
  }

  private fun createAnswerIdeWorkspaceIdentifier(answerId: UUID): WorkspaceIdentifier {
    return WorkspaceIdentifier(
      purpose = WorkspacePurpose.ANSWER_IDE,
      reference = answerId.toString()
    )
  }

  private fun createAnswerIdeWorkspaceConfig(): WorkspaceConfiguration {
    return WorkspaceConfiguration(
      scripts = emptyMap(),
      imageName = defaultWorkspaceImage
    )
  }
}
