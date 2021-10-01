package org.codefreak.codefreak.cloud

import com.fasterxml.jackson.databind.ObjectMapper
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientTimeoutException
import io.fabric8.kubernetes.client.dsl.CreateOrReplaceable
import io.fabric8.kubernetes.client.dsl.MultiDeleteable
import org.apache.commons.io.IOUtils
import org.codefreak.codefreak.cloud.model.CompanionIngress
import org.codefreak.codefreak.cloud.model.CompanionPod
import org.codefreak.codefreak.cloud.model.CompanionScriptMap
import org.codefreak.codefreak.cloud.model.CompanionService
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.service.file.FileService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class KubernetesWorkspaceService(
    private val kubernetesClient: KubernetesClient,
    private val config: AppConfiguration,
    private val fileService: FileService,
    private val wsClientFactory: WorkspaceClientFactory
) : WorkspaceService<KubernetesWorkspaceConfig> {
  companion object {
    private val log = LoggerFactory.getLogger(KubernetesWorkspaceService::class.java)
  }

  @Autowired
  @Qualifier("yamlObjectMapper")
  private lateinit var yamlMapper: ObjectMapper

  override fun createWorkspace(config: KubernetesWorkspaceConfig): WorkspaceReference {
    val existingWorkspace = findWorkspace(config)
    if (existingWorkspace != null) {
      log.debug("Workspace ${config.workspaceId} already exists.")
      return existingWorkspace
    }
    log.info("Creating new workspace ${config.workspaceId}")

    // From this point on we will start creating the required K8s resources
    try {
      kubernetesClient.configMaps().createOrReplaceWithLog(CompanionScriptMap(config))
      kubernetesClient.pods().createOrReplaceWithLog(CompanionPod(config))
      kubernetesClient.services().createOrReplaceWithLog(CompanionService(config))
      val ingress = CompanionIngress(config)
      kubernetesClient.network().v1().ingresses().createOrReplaceWithLog(ingress)

      // make sure the deployment is ready
      val companionPod = kubernetesClient.pods().withName(config.companionDeploymentName)
      try {
        companionPod.waitUntilReady(20, TimeUnit.SECONDS)
        log.debug("Workspace ${config.workspaceId} is ready.")
      } catch (e: KubernetesClientTimeoutException) {
        throw IllegalStateException("Workspace ${config.workspaceId} is not ready after 20sec.")
      }

      // deploy answer files
      val reference = createStaticReference(config)
      val wsClient = wsClientFactory.createClient(reference)
      if (!wsClient.waitForWorkspaceToComeLive(10L, TimeUnit.SECONDS)) {
        throw IllegalStateException("Workspace ${config.workspaceId} is not reachable at ${ingress.getBaseUrl()} after 10sec even though the Pod is ready.")
      }
      log.debug("Deploying files to workspace ${config.workspaceId}...")
      config.files.use(wsClient::deployFiles)
      log.debug("Deployed files to workspace ${config.workspaceId}!")
      return reference
    } catch (e: Exception) {
      // make sure we tear down the workspace in case something went wrong during deployment
      deleteWorkspaceResources(config)
      throw e
    }
  }

  override fun deleteWorkspace(config: KubernetesWorkspaceConfig) {
    val reference = createStaticReference(config)
    val wsClient = wsClientFactory.createClient(reference)
    if (!config.isReadOnly) {
      log.debug("Saving files of workspace ${config.workspaceId}!")
      wsClient.downloadTar(consumer = config::saveFiles)
    } else {
      log.debug("Not saving files of workspace ${config.workspaceId} because it is read-only")
    }
    deleteWorkspaceResources(config)
  }

  private fun deleteWorkspaceResources(config: KubernetesWorkspaceConfig) {
    kubernetesClient.services().deleteWithLog(CompanionService(config))
    kubernetesClient.network().v1().ingresses().deleteWithLog(CompanionIngress(config))
    kubernetesClient.pods().deleteWithLog(CompanionPod(config))
    kubernetesClient.configMaps().deleteWithLog(CompanionScriptMap(config))
  }

  fun createWorkspaceConfigForCollection(collectionId: UUID): KubernetesWorkspaceConfig {
    return KubernetesWorkspaceConfig(
        config,
        collectionId,
        saveFilesFunction = { newFiles ->
          fileService.writeCollectionTar(collectionId).use {
            IOUtils.copy(newFiles, it)
          }
        },
        filesSupplier = { fileService.readCollectionTar(collectionId) }
    )
  }

  override fun findWorkspace(config: KubernetesWorkspaceConfig): WorkspaceReference? {
    if (kubernetesClient.apps().deployments().withName(config.companionDeploymentName).get() != null) {
      return createStaticReference(config)
    }
    return null
  }

  private fun createStaticReference(config: KubernetesWorkspaceConfig): WorkspaceReference {
    val ingress = CompanionIngress(config)
    return DefaultWorkspaceReference(
        id = config.externalId,
        baseUrl = ingress.getBaseUrl(),
        authToken = ""
    )
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
