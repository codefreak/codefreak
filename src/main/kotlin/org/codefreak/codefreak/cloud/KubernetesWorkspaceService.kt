package org.codefreak.codefreak.cloud

import com.fasterxml.jackson.databind.ObjectMapper
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientTimeoutException
import io.fabric8.kubernetes.client.dsl.CreateOrReplaceable
import io.fabric8.kubernetes.client.dsl.PodResource
import java.net.URI
import java.util.concurrent.TimeUnit
import org.codefreak.codefreak.cloud.model.WorkspaceIngressModel
import org.codefreak.codefreak.cloud.model.WorkspacePodModel
import org.codefreak.codefreak.cloud.model.WorkspaceScriptMapModel
import org.codefreak.codefreak.cloud.model.WorkspaceServiceModel
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.withTrailingSlash
import org.codefreak.codefreak.util.withoutTrailingSlash
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils

@Service
class KubernetesWorkspaceService(
  private val kubernetesClient: KubernetesClient,
  private val appConfig: AppConfiguration,
  private val fileService: FileService,
  private val wsClientFactory: WorkspaceClientFactory
) : WorkspaceService {
  companion object {
    private val log = LoggerFactory.getLogger(KubernetesWorkspaceService::class.java)
  }

  @Autowired
  @Qualifier("yamlObjectMapper")
  private lateinit var yamlMapper: ObjectMapper

  override fun createWorkspace(identifier: WorkspaceIdentifier, config: WorkspaceConfiguration): RemoteWorkspaceReference {
    if (getWorkspacePod(identifier).get() != null) {
      log.debug("Workspace $identifier already exists.")
      return createReference(identifier)
    }
    val wsHash = identifier.hashString()
    log.info("Creating new workspace $identifier")

    // From this point on we will start creating the required K8s resources
    try {
      createWorkspaceResources(identifier, config)

      // make sure the deployment is ready
      val companionPod = getWorkspacePod(identifier)
      try {
        companionPod.waitUntilReady(20, TimeUnit.SECONDS)
        log.debug("Workspace Pod $wsHash is ready")
      } catch (e: KubernetesClientTimeoutException) {
        throw IllegalStateException("Workspace $wsHash is not ready after 20sec.")
      }

      // deploy answer files
      val reference = createReference(identifier)
      val wsClient = wsClientFactory.createClient(reference)
      if (!wsClient.waitForWorkspaceToComeLive(10L, TimeUnit.SECONDS)) {
        throw IllegalStateException("Workspace $wsHash is not reachable at ${reference.baseUrl} after 10sec even though the Pod is ready.")
      }
      log.debug("Deploying files to workspace $wsHash...")
      fileService.readCollectionTar(config.collectionId).use(wsClient::deployFiles)
      log.debug("Deployed files to workspace $wsHash!")
      return reference
    } catch (e: Exception) {
      // make sure we tear down the workspace in case something went wrong during deployment
      deleteWorkspaceResources(identifier)
      throw e
    }
  }

  private fun getWorkspacePod(identifier: WorkspaceIdentifier): PodResource<Pod?> {
    return kubernetesClient.pods().withName(identifier.workspacePodName)
  }

  override fun deleteWorkspace(identifier: WorkspaceIdentifier) {
    val pod = getWorkspacePod(identifier).get()
    if (pod == null) {
      log.debug("Attempted to delete workspace that is not existing: $identifier")
      return
    }
    saveWorkspaceFiles(identifier)
    deleteWorkspaceResources(identifier)
  }

  override fun saveWorkspaceFiles(identifier: WorkspaceIdentifier) {
    val pod = getWorkspacePod(identifier).get() ?: return

    val reference = createReference(identifier)
    val wsClient = wsClientFactory.createClient(reference)
    if (!pod.isReadOnly) {
      log.debug("Saving files of workspace $identifier!")
      wsClient.downloadTar { newArchive ->
        fileService.writeCollectionTar(pod.collectionId).use {
          StreamUtils.copy(newArchive, it)
        }
      }
    } else {
      log.debug("Not saving files of workspace $identifier because it is read-only")
    }
  }

  private fun createWorkspaceResources(identifier: WorkspaceIdentifier, config: WorkspaceConfiguration) {
    kubernetesClient.configMaps().createOrReplaceWithLog(WorkspaceScriptMapModel(identifier, config))
    kubernetesClient.pods().createOrReplaceWithLog(WorkspacePodModel(identifier, config))
    kubernetesClient.services().createOrReplaceWithLog(WorkspaceServiceModel(identifier))
    kubernetesClient.network().v1().ingresses()
      .createOrReplaceWithLog(WorkspaceIngressModel(identifier, buildWorkspaceBaseUrl(identifier)))
  }

  private fun deleteWorkspaceResources(identifier: WorkspaceIdentifier) {
    log.debug("Deleting workspace resources of $identifier")
    kubernetesClient.services().withName(identifier.workspaceServiceName).delete()
    kubernetesClient.network().v1().ingresses().withName(identifier.workspaceIngressName).delete()
    kubernetesClient.pods().withName(identifier.workspacePodName).delete()
    kubernetesClient.configMaps().withName(identifier.workspaceScriptMapName).delete()
  }

  override fun findAllWorkspaces(): List<RemoteWorkspaceReference> {
    return kubernetesClient.pods()
      .withLabel(WS_K8S_LABEL_REFERENCE)
      .list().items
      .map { createReference(it.toWorkspaceIdentifier) }
  }

  private fun createReference(identifier: WorkspaceIdentifier): RemoteWorkspaceReference {
    return RemoteWorkspaceReference(
      id = identifier,
      baseUrl = buildWorkspaceBaseUrl(identifier).toString(),
      authToken = ""
    )
  }

  private fun buildWorkspaceBaseUrl(identifier: WorkspaceIdentifier): URI {
    val globalBase = appConfig.workspaces.baseUrl
    return URI(
      globalBase.scheme,
      null, // user info
      globalBase.host,
      globalBase.port,
      (globalBase.path.withTrailingSlash() + identifier.hashString()).withoutTrailingSlash(),
      null, // query string
      null // fragment
    )
  }

  private fun <T> CreateOrReplaceable<T>.createOrReplaceWithLog(vararg items: T): T {
    log.debug("Creating or replacing:\n${items.joinToString(separator = "\n---\n") { yamlMapper.writeValueAsString(it) }}")
    return createOrReplace(*items)
  }
}
