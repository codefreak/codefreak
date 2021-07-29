package org.codefreak.codefreak.cloud.model

import com.fkorotkov.kubernetes.metadata
import io.fabric8.kubernetes.api.model.ConfigMap
import org.codefreak.codefreak.cloud.WorkspaceConfiguration

class CompanionScriptMap(wsConfig: WorkspaceConfiguration): ConfigMap() {
  init {
    metadata {
      name = wsConfig.companionScriptMapName
      labels = wsConfig.getLabelsForComponent("companion")
      data = wsConfig.scripts.toMap()
    }
  }
}
