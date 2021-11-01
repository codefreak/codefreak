package org.codefreak.codefreak.service.workspace.model

import com.fkorotkov.kubernetes.configMap
import com.fkorotkov.kubernetes.emptyDir
import com.fkorotkov.kubernetes.httpGet
import com.fkorotkov.kubernetes.livenessProbe
import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newContainer
import com.fkorotkov.kubernetes.newContainerPort
import com.fkorotkov.kubernetes.newEnvVar
import com.fkorotkov.kubernetes.newKeyToPath
import com.fkorotkov.kubernetes.newVolume
import com.fkorotkov.kubernetes.newVolumeMount
import com.fkorotkov.kubernetes.readinessProbe
import com.fkorotkov.kubernetes.resources
import com.fkorotkov.kubernetes.spec
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.Quantity
import org.codefreak.codefreak.service.workspace.WorkspaceConfiguration
import org.codefreak.codefreak.service.workspace.WorkspaceIdentifier
import org.codefreak.codefreak.service.workspace.k8sLabels
import org.codefreak.codefreak.service.workspace.workspacePodName
import org.codefreak.codefreak.service.workspace.workspaceScriptMapName
import java.util.Objects

class WorkspacePodModel(
  identifier: WorkspaceIdentifier,
  wsConfig: WorkspaceConfiguration,
  springApplicationConfig: String
) : Pod() {
  init {
    metadata {
      name = identifier.workspacePodName
      labels = identifier.k8sLabels
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

        resources {
          requests = mapOf(
            "cpu" to wsConfig.cpuLimit?.let(Quantity::parse),
            "memory" to wsConfig.memoryLimit?.let(Quantity::parse),
            "ephemeral-storage" to wsConfig.diskLimit?.let(Quantity::parse)
          ).filterValues(Objects::nonNull)
          limits = mapOf(
            "cpu" to wsConfig.cpuLimit?.let(Quantity::parse),
            "memory" to wsConfig.memoryLimit?.let(Quantity::parse),
            "ephemeral-storage" to wsConfig.diskLimit?.let(Quantity::parse)
          ).filterValues(Objects::nonNull)
        }

        // disable environment variables with service links
        enableServiceLinks = false
        // apply custom environment variables first
        env = wsConfig.environment?.map { (key, value) ->
          newEnvVar {
            name = key
            this.value = value
          }
        } ?: emptyList()
        // override them with our necessary environment variables
        env = env + listOf(
          newEnvVar {
            name = "JAVA_OPTS"
            value = "-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xshareclasses -Xquickstart"
          },
          newEnvVar {
            name = "SPRING_APPLICATION_JSON"
            value = springApplicationConfig
          }
        )
        volumeMounts = listOf(
          newVolumeMount {
            name = "project-files"
            mountPath = "/home/runner/project"
          },
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
          name = "project-files"
          emptyDir {
            // sizeLimit is set via spec.containers[].resources.limits.ephemeral-storage
          }
        },
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
