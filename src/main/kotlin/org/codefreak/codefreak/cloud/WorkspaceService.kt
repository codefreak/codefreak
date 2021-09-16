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
import org.codefreak.codefreak.service.file.FileService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils

@Service
class WorkspaceService(
  private val kubernetesClient: KubernetesClient,
  private val config: AppConfiguration,
  private val fileService: FileService,
  private val wsClientFactory: WorkspaceClientFactory
) {
  companion object {
    private val log = LoggerFactory.getLogger(WorkspaceService::class.java)
  }

  @Autowired
  @Qualifier("yamlObjectMapper")
  private lateinit var yamlMapper: ObjectMapper

  fun createWorkspace(wsConfig: WorkspaceConfiguration): RemoteWorkspaceReference {
    val ingress = CompanionIngress(wsConfig)
    val reference = RemoteWorkspaceReference(
        baseUrl = ingress.getBaseUrl(),
        authToken = ""
    )
    if (kubernetesClient.apps().deployments().withName(wsConfig.companionDeploymentName).get() != null) {
      log.debug("Workspace ${wsConfig.workspaceId} already exists.")
      return reference
    }
    log.info("Creating new workspace ${wsConfig.workspaceId}")

    // deploy the companion
    kubernetesClient.configMaps().createOrReplaceWithLog(CompanionScriptMap(wsConfig))
    kubernetesClient.apps().deployments().createOrReplaceWithLog(CompanionDeployment(wsConfig))
    kubernetesClient.services().createOrReplaceWithLog(CompanionService(wsConfig))
    kubernetesClient.network().v1().ingresses().createOrReplaceWithLog(ingress)

    // deploy answer files
    val wsClient = wsClientFactory.createClient(reference)
    log.debug("Waiting for workspace ${wsConfig.workspaceId} to come live")
    while (!wsClient.isWorkspaceLive()) {
      log.debug("Workspace ${wsConfig.workspaceId} still not live...")
      Thread.sleep(1000L)
    }
    log.debug("Workspace ${wsConfig.workspaceId} is live!")
    log.debug("Deploying files to workspace ${wsConfig.workspaceId}...")
    fileService.readCollectionTar(UUID.fromString(wsConfig.workspaceId)).use {
      wsClient.deployFiles(it)
    }
    log.debug("Deployed files to workspace ${wsConfig.workspaceId}!")

    return reference
  }

  fun deleteWorkspace(wsConfig: WorkspaceConfiguration) {
    val ingress = CompanionIngress(wsConfig)
    val reference = RemoteWorkspaceReference(
        baseUrl = ingress.getBaseUrl(),
        authToken = ""
    )
    val wsClient = wsClientFactory.createClient(reference)
    log.debug("Saving files of workspace ${wsConfig.workspaceId}!")
    wsClient.downloadTar { downloadedFiles ->
      fileService.writeCollectionTar(UUID.fromString(wsConfig.workspaceId)).use { tarCollection ->
        StreamUtils.copy(downloadedFiles, tarCollection)
      }
    }
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
