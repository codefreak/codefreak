package org.codefreak.codefreak.cloud.model

import com.fkorotkov.kubernetes.metadata
import io.fabric8.kubernetes.api.model.ConfigMap
import org.codefreak.codefreak.cloud.WorkspaceConfiguration
import org.codefreak.codefreak.cloud.WorkspaceIdentifier
import org.codefreak.codefreak.cloud.k8sLabels
import org.codefreak.codefreak.cloud.workspaceScriptMapName

class WorkspaceScriptMapModel(identifier: WorkspaceIdentifier, wsConfig: WorkspaceConfiguration) : ConfigMap() {
  init {
    metadata {
      name = identifier.workspaceScriptMapName
      labels = identifier.k8sLabels
      data = wsConfig.scripts.toMap()
    }
  }
}
