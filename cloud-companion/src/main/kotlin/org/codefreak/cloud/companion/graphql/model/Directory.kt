package org.codefreak.cloud.companion.graphql.model

data class Directory(
  override val path: String
) : FileSystemNode
