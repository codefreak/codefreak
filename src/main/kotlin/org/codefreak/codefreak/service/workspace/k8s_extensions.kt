package org.codefreak.codefreak.service.workspace

import io.fabric8.kubernetes.api.model.Pod

const val WS_K8S_LABEL_REFERENCE = "org.codefreak.reference"
const val WS_K8S_LABEL_PURPOSE = "org.codefreak.purpose"

val WorkspaceIdentifier.k8sLabels
  get() = mapOf(
    WS_K8S_LABEL_REFERENCE to reference,
    WS_K8S_LABEL_PURPOSE to purpose.key
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

// Service names are limited to 63 characters and must be a valid DNS-1035 identifier...
val WorkspaceIdentifier.workspaceServiceName: String
  get() = hashString().let {
    if (it.length > 63) {
      it.substring(0..63)
    } else {
      it
    }.trim('-') // cannot end with slash
  }

val WorkspaceIdentifier.workspaceScriptMapName: String
  get() = hashString()

val WorkspaceIdentifier.workspacePodName: String
  get() = hashString()

val WorkspaceIdentifier.workspaceIngressName: String
  get() = hashString()
