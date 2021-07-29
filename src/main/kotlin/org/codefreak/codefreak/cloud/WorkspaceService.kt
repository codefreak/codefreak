package org.codefreak.codefreak.cloud

import com.fasterxml.jackson.databind.ObjectMapper
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.CreateOrReplaceable
import io.fabric8.kubernetes.client.dsl.MultiDeleteable
import java.util.UUID
import org.codefreak.codefreak.cloud.model.CompanionDeployment
import org.codefreak.codefreak.cloud.model.CompanionIngress
import org.codefreak.codefreak.cloud.model.CompanionScriptMap
import org.codefreak.codefreak.cloud.model.CompanionService
import org.codefreak.codefreak.cloud.model.WorkspacePersistentVolumeClaim
import org.codefreak.codefreak.config.AppConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class WorkspaceService(
  private val kubernetesClient: KubernetesClient,
  private val config: AppConfiguration
) {
  companion object {
    private val log = LoggerFactory.getLogger(WorkspaceService::class.java)
  }

  @Autowired
  @Qualifier("yamlObjectMapper")
  private lateinit var yamlMapper: ObjectMapper

  fun createWorkspace(wsConfig: WorkspaceConfiguration): RemoteWorkspaceReference {
    // PVCs are immutable after they have been created. Thus we have to check if the PVC already exists.
    if (kubernetesClient.persistentVolumeClaims().withName(wsConfig.persistentVolumeClaimName).get() == null) {
      kubernetesClient.persistentVolumeClaims().createOrReplaceWithLog(WorkspacePersistentVolumeClaim(wsConfig))
    } else {
      log.debug("PVC ${wsConfig.persistentVolumeClaimName} already exists")
    }

    // deploy the companion
    kubernetesClient.configMaps().createOrReplaceWithLog(CompanionScriptMap(wsConfig))
    kubernetesClient.apps().deployments().createOrReplaceWithLog(CompanionDeployment(wsConfig))
    kubernetesClient.services().createOrReplaceWithLog(CompanionService(wsConfig))
    val ingress = CompanionIngress(wsConfig)
    kubernetesClient.network().v1().ingresses().createOrReplaceWithLog(ingress)

    return RemoteWorkspaceReference(
        baseUrl = ingress.getBaseUrl(),
        authToken = ""
    )
  }

  fun deleteWorkspace(wsConfig: WorkspaceConfiguration) {
    // delete the companion deployment
    kubernetesClient.services().deleteWithLog(CompanionService(wsConfig))
    kubernetesClient.network().v1().ingresses().deleteWithLog(CompanionIngress(wsConfig))
    kubernetesClient.apps().deployments().deleteWithLog(CompanionDeployment(wsConfig))
    kubernetesClient.configMaps().deleteWithLog(CompanionScriptMap(wsConfig))

    // delete the pvc
    kubernetesClient.persistentVolumeClaims().deleteWithLog(WorkspacePersistentVolumeClaim(wsConfig))
  }

  fun createWorkspaceConfigForCollection(collectionId: UUID): WorkspaceConfiguration {
    return WorkspaceConfiguration(config, collectionId)
  }

  private fun <T> CreateOrReplaceable<T>.createOrReplaceWithLog(vararg items: T): T {
    log.debug("Creating or replacing:\n${items.joinToString(separator = "\n---\n") { yamlMapper.writeValueAsString(it) }}")
    return createOrReplace(*items)
  }

  private fun <T> MultiDeleteable<T>.deleteWithLog(vararg items: T) {
    log.debug("Deleting:\n${items.joinToString(separator = "\n---\n") { yamlMapper.writeValueAsString(it) }}")
    delete(*items)
  }
}
