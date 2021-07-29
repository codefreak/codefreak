package org.codefreak.codefreak.cloud.model

import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.resources
import com.fkorotkov.kubernetes.spec
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim
import io.fabric8.kubernetes.api.model.Quantity
import org.codefreak.codefreak.cloud.WorkspaceConfiguration

class WorkspacePersistentVolumeClaim(wsConfig: WorkspaceConfiguration) : PersistentVolumeClaim() {
  init {
    metadata {
      name = wsConfig.persistentVolumeClaimName
      labels = wsConfig.getLabels()
    }
    spec {
      accessModes = listOf("ReadWriteMany")
      volumeMode = "Filesystem"
      // volumeName = wsConfig.persistentVolumeName
      storageClassName = "standard"
      resources {
        requests = mapOf(
            "storage" to Quantity("1Gi")
        )
      }
    }
  }
}
