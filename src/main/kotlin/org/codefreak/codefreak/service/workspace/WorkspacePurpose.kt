package org.codefreak.codefreak.service.workspace

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
