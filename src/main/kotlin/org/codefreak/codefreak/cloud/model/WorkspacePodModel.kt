package org.codefreak.codefreak.cloud.model

import com.fkorotkov.kubernetes.configMap
import com.fkorotkov.kubernetes.httpGet
import com.fkorotkov.kubernetes.livenessProbe
import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newContainer
import com.fkorotkov.kubernetes.newContainerPort
import com.fkorotkov.kubernetes.newKeyToPath
import com.fkorotkov.kubernetes.newVolume
import com.fkorotkov.kubernetes.newVolumeMount
import com.fkorotkov.kubernetes.readinessProbe
import com.fkorotkov.kubernetes.spec
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.Pod
import org.codefreak.codefreak.cloud.WorkspaceConfiguration
import org.codefreak.codefreak.cloud.WorkspaceIdentifier
import org.codefreak.codefreak.cloud.k8sLabels
import org.codefreak.codefreak.cloud.workspacePodName
import org.codefreak.codefreak.cloud.workspaceScriptMapName

class WorkspacePodModel(identifier: WorkspaceIdentifier, wsConfig: WorkspaceConfiguration) : Pod() {
  init {
    metadata {
      name = identifier.workspacePodName
      // TODO: Store configuration in secret
      labels = identifier.k8sLabels + wsConfig.k8sLabels
    }
    spec {
      containers = listOf(newContainer {
        name = "companion"
        image = wsConfig.imageName
        imagePullPolicy = "IfNotPresent"
        ports = listOf(newContainerPort {
          name = "http"
          containerPort = 8080
          protocol = "TCP"
        })
        // resources {
        //  requests = mapOf(
        //      "cpu" to Quantity.parse("1"),
        //      "memory" to Quantity.parse("128Mi")
        //  )
        //  limits = mapOf(
        //      "cpu" to Quantity.parse("4"),
        //      "memory" to Quantity.parse("512Mi")
        //  )
        // }
        // disable environment variables with service links
        enableServiceLinks = false
        volumeMounts = listOf(
            newVolumeMount {
              name = "scripts"
              mountPath = "/scripts"
              readOnly = true
            }
        )
        livenessProbe {
          httpGet {
            path = "/actuator/health/liveness"
            port = IntOrString("http")
          }
          failureThreshold = 10
          initialDelaySeconds = 1
          periodSeconds = 1
        }
        readinessProbe {
          httpGet {
            path = "/actuator/health/readiness"
            port = IntOrString("http")
          }
          failureThreshold = 20
          initialDelaySeconds = 1
          periodSeconds = 1
        }
      })
      volumes = listOf(
          newVolume {
            name = "scripts"
            configMap {
              name = identifier.workspaceScriptMapName
              defaultMode = 493 // equals 0755
              items = wsConfig.scripts.map { (scriptName) ->
                newKeyToPath {
                  key = scriptName
                  path = scriptName
                }
              }
            }
          }
      )
    }
  }
}
