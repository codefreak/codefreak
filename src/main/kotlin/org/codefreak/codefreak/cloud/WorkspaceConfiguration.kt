package org.codefreak.codefreak.cloud

import java.util.UUID

/**
 * Defines various purposes a workspace can be used for.
 * The purpose is used for the identification of a workspace.
 */
enum class WorkspacePurpose(val key: String) {
  EVALUATION("evaluation"),
  ANSWER_IDE("answer"),
  TASK_IDE("task");

  companion object {
    fun fromKey(key: String): WorkspacePurpose = when (key) {
      "evaluation" -> EVALUATION
      "answer" -> ANSWER_IDE
      "task" -> TASK_IDE
      else -> throw IllegalArgumentException("Invalid workspace purpose key $key")
    }
  }
}

/**
 * ID that identifies the workspace uniquely.
 * Implementations will not create more than one workspace instance for the same set
 * of identifiers.
 */
data class WorkspaceIdentifier(
  /**
   * Purpose of the workspace. The combination of purpose + reference
   * will make the identifier unique.
   */
  val purpose: WorkspacePurpose,

  /**
   * ID of the object this workspace refers to.
   * This will either be:
   * - The ID of an evaluation step
   * - The ID of an answer
   * - The ID of a task
   * The same reference could be used for different purposes, e.g. the student is working
   * on his answer or the teacher could start a read-only IDE for the same answer.
   */
  val reference: String
) {
  /**
   * Creates a unique string that contains all aspects of the identifier.
   * Implementations should use this string to identify resources of a workspace.
   */
  fun hashString(): String {
    return "${purpose.key}-$reference"
  }
}

/**
 * Interface for describing the demand for a Workspace
 */
interface WorkspaceConfiguration {
  /**
   * Identity of the user that will be authorized to access the workspace.
   * This should be a unique identity of the user (id or username).
   * Will be used for creating the JWT "sub" claim.
   */
  val user: String

  /**
   * Collection-ID that will be used for reading/writing files from/to a workspace.
   * If the workspace is marked as read-only the collection will only be extracted
   * and not stored back to the database.
   */
  val collectionId: UUID

  /**
   * Map of executable name to content of scripts that will be added to PATH
   */
  val scripts: Map<String, String>

  /**
   * Name of the container image that will be used for creating the workspace.
   * The default value will be the current companion image but teachers might be able to create
   * their own custom images in the future.
   */
  val imageName: String

  /**
   * Indicate if this workspace is read-only, meaning the files will not be saved
   * back to the database.
   */
  val isReadOnly: Boolean
}

/**
 * Reference to a workspace that has been created by an implementation.
 * It contains all information that is required to connect to a workspace.
 */
data class RemoteWorkspaceReference(
  val id: WorkspaceIdentifier,
  val baseUrl: String,
  val authToken: String
)

interface WorkspaceService {
  /**
   * Create a workspaces with the given identifier. If a workspace already exist it should not create a new one
   * but return a reference to the already existing one.
   * If there is no existing workspace create a new one with the given configuration.
   */
  fun createWorkspace(identifier: WorkspaceIdentifier, config: WorkspaceConfiguration): RemoteWorkspaceReference
  fun deleteWorkspace(identifier: WorkspaceIdentifier)
  fun findAllWorkspaces(): List<RemoteWorkspaceReference>

  /**
   * Trigger a file save on the given workspace. This will take the current file state from
   * the workspace and update the collection this workspace is based on in the database.
   * Implementations should not perform any updates if this workspace is marked as read-only.
   */
  fun saveWorkspaceFiles(identifier: WorkspaceIdentifier)
}
