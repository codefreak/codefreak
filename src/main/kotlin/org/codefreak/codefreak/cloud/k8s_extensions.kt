package org.codefreak.codefreak.cloud

import io.fabric8.kubernetes.api.model.Pod
import java.util.UUID

const val WS_K8S_LABEL_REFERENCE = "org.codefreak.reference"
const val WS_K8S_LABEL_PURPOSE = "org.codefreak.purpose"
const val WS_K8S_LABEL_COLLECTION_ID = "org.codefreak.collection-id"
const val WS_K8S_LABEL_READONLY = "org.codefreak.read-only"

val WorkspaceIdentifier.k8sLabels
  get() = mapOf(
    WS_K8S_LABEL_REFERENCE to reference,
    WS_K8S_LABEL_PURPOSE to purpose.key
  )

/**
 * TODO: Store configuration in secret and not as labels
 */
val WorkspaceConfiguration.k8sLabels
  get() = mapOf(
    WS_K8S_LABEL_COLLECTION_ID to collectionId.toString(),
    WS_K8S_LABEL_READONLY to if (isReadOnly) "true" else "false"
  )

val Pod.reference: String
  get() = this.metadata.labels[WS_K8S_LABEL_REFERENCE]
    ?: throw IllegalStateException("Given pod ${this.metadata.name} does not look like a workspace pod")

val Pod.purpose: String
  get() = this.metadata.labels[WS_K8S_LABEL_PURPOSE]
    ?: throw IllegalStateException("Given pod ${this.metadata.name} does not look like a workspace pod")

val Pod.toWorkspaceIdentifier: WorkspaceIdentifier
  get() = WorkspaceIdentifier(
    purpose = WorkspacePurpose.fromKey(purpose),
    reference = reference
  )

val Pod.isReadOnly: Boolean
  get() = this.metadata.labels[WS_K8S_LABEL_READONLY]?.let { it == "true" }
    ?: throw IllegalStateException("Given pod ${this.metadata.name} does not look like a workspace pod")

val Pod.collectionId: UUID
  get() = this.metadata.labels[WS_K8S_LABEL_COLLECTION_ID]?.let { UUID.fromString(it) }
    ?: throw IllegalStateException("Given pod ${this.metadata.name} does not look like a workspace pod")

// Service names are limited to 63 characters and must be a valid DNS-1035 identifier...
val WorkspaceIdentifier.workspaceServiceName: String
  get() = hashString().substring(0..32).trim('-')

val WorkspaceIdentifier.workspaceScriptMapName: String
  get() = hashString()

val WorkspaceIdentifier.workspacePodName: String
  get() = hashString()

val WorkspaceIdentifier.workspaceIngressName: String
  get() = hashString()
