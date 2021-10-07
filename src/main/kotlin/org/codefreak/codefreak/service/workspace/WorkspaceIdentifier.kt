package org.codefreak.codefreak.service.workspace

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
