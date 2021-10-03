package org.codefreak.codefreak.cloud

import java.util.UUID

data class DefaultWorkspaceConfiguration(
  private val reference: UUID,
  override val collectionId: UUID,
  override val isReadOnly: Boolean,
  override val imageName: String
) : WorkspaceConfiguration {
  override val user = ""
  override val scripts: MutableMap<String, String> = mutableMapOf()

  fun addScript(name: String, content: String) {
    scripts[name] = content
  }
}
