package org.codefreak.codefreak.service.workspace.model

import com.fkorotkov.kubernetes.metadata
import io.fabric8.kubernetes.api.model.ConfigMap
import org.codefreak.codefreak.service.workspace.WorkspaceConfiguration
import org.codefreak.codefreak.service.workspace.WorkspaceIdentifier
import org.codefreak.codefreak.service.workspace.k8sLabels
import org.codefreak.codefreak.service.workspace.workspaceScriptMapName

class WorkspaceScriptMapModel(identifier: WorkspaceIdentifier, wsConfig: WorkspaceConfiguration) : ConfigMap() {
  init {
    metadata {
      name = identifier.workspaceScriptMapName
      labels = identifier.k8sLabels
      data = wsConfig.scripts.toMap()
    }
  }
}
