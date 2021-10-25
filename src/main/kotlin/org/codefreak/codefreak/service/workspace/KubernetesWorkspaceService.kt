package org.codefreak.codefreak.service.workspace

import com.fasterxml.jackson.databind.ObjectMapper
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientTimeoutException
import io.fabric8.kubernetes.client.dsl.CreateOrReplaceable
import io.fabric8.kubernetes.client.dsl.PodResource
import java.net.URI
import java.util.Objects
import java.util.concurrent.TimeUnit.SECONDS
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.service.workspace.model.WorkspaceIngressModel
import org.codefreak.codefreak.service.workspace.model.WorkspacePodModel
import org.codefreak.codefreak.service.workspace.model.WorkspaceScriptMapModel
import org.codefreak.codefreak.service.workspace.model.WorkspaceServiceModel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

@Service
class KubernetesWorkspaceService(
  private val kubernetesClient: KubernetesClient,
  private val appConfig: AppConfiguration,
  private val fileService: WorkspaceFileService,
  private val workspaceClientService: WorkspaceClientService,
  @Qualifier("yamlObjectMapper")
  private val yamlMapper: ObjectMapper,
  private val jsonMapper: ObjectMapper
) : WorkspaceService {
  companion object {
    private val log = LoggerFactory.getLogger(KubernetesWorkspaceService::class.java)
  }

  override fun createWorkspace(
    identifier: WorkspaceIdentifier,
    config: WorkspaceConfiguration
  ): RemoteWorkspaceReference {
    if (getWorkspacePodResource(identifier).get() != null) {
      log.debug("Workspace $identifier already exists.")
      return createReference(identifier)
    }
    val wsHash = identifier.hashString()
    log.info("Creating new workspace $identifier")

    // From this point on we will start creating the required K8s resources
    try {
      createWorkspaceResources(identifier, config)

      // make sure the deployment is ready
      val podResource = getWorkspacePodResource(identifier)
      try {
        podResource.waitUntilReady(20, SECONDS)
        log.debug("Workspace Pod $wsHash is ready")
      } catch (e: KubernetesClientTimeoutException) {
        throw IllegalStateException("Workspace $wsHash is not ready after 20sec.")
      }
      val companionPod = podResource.get() ?: throw IllegalStateException("Pod not existing after creation")

      // deploy answer files
      val reference = createReference(identifier)
      val wsClient = workspaceClientService.getClient(reference)
      if (!wsClient.waitForWorkspaceToComeLive(10L, SECONDS)) {
        val podLog = kubernetesClient.pods().withName(identifier.workspacePodName).log
        throw IllegalStateException("Workspace $wsHash is not reachable at ${reference.baseUrl} after 10sec even though the Pod is ready. Pods log: \n$podLog")
      }
      deployWorkspaceFiles(companionPod, wsClient)
      return reference
    } catch (e: Exception) {
      // make sure we tear down the workspace in case something went wrong during deployment
      deleteWorkspaceResources(identifier)
      throw e
    }
  }

  private fun deployWorkspaceFiles(workspacePod: Pod, wsClient: WorkspaceClient) {
    log.debug("Deploying files to workspace ${workspacePod.reference}...")
    try {
      fileService.loadFiles(workspacePod.toWorkspaceIdentifier, wsClient::deployFiles)
    } catch (e: RuntimeException) {
      throw IllegalStateException("Could not deploy files to workspace ${workspacePod.reference}", e)
    }
    log.debug("Deployed files to workspace ${workspacePod.reference}!")
  }

  private fun getWorkspacePodResource(identifier: WorkspaceIdentifier): PodResource<Pod?> {
    return kubernetesClient.pods().withName(identifier.workspacePodName)
  }

  override fun deleteWorkspace(identifier: WorkspaceIdentifier) {
    val pod = getWorkspacePodResource(identifier).get()
    if (pod == null) {
      log.debug("Attempted to delete workspace that is not existing: $identifier")
      return
    }
    saveWorkspaceFiles(identifier)
    deleteWorkspaceResources(identifier)
  }

  override fun saveWorkspaceFiles(identifier: WorkspaceIdentifier) {
    val pod = getWorkspacePodResource(identifier).get()
    if (pod == null) {
      log.debug("Not saving workspace files for identifier $identifier because workspace does not exist")
      return
    }

    val reference = createReference(identifier)
    val wsClient = workspaceClientService.getClient(reference)
    log.debug("Delegating file save of workspace $identifier!")
    wsClient.downloadTar { newArchive ->
      fileService.saveFiles(identifier) { newArchive }
    }
  }

  override fun redeployWorkspaceFiles(identifier: WorkspaceIdentifier) {
    val pod = getWorkspacePodResource(identifier).get()
    if (pod == null) {
      log.debug("Not refreshing workspace files for identifier $identifier because workspace does not exist")
      return
    }
    val wsClient = workspaceClientService.getClient(createReference(identifier))
    log.debug("Refreshing files of workspace $identifier!")
    deployWorkspaceFiles(pod, wsClient)
  }

  private fun createWorkspaceResources(identifier: WorkspaceIdentifier, config: WorkspaceConfiguration) {
    kubernetesClient.configMaps().createOrReplaceWithLog(WorkspaceScriptMapModel(identifier, config))
    kubernetesClient.pods().createOrReplaceWithLog(
      WorkspacePodModel(identifier, config, buildCompanionSpringConfig(identifier))
    )
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
    // wait that pod is gone
    kubernetesClient.pods()
      .withName(identifier.workspacePodName)
      .waitUntilCondition(Objects::isNull, 20, SECONDS)
  }

  override fun findAllWorkspaces(): List<RemoteWorkspaceReference> {
    return kubernetesClient.pods()
      .withLabel(WS_K8S_LABEL_REFERENCE)
      .list().items
      .map { createReference(it.toWorkspaceIdentifier) }
  }

  private fun createReference(identifier: WorkspaceIdentifier): RemoteWorkspaceReference {
    return RemoteWorkspaceReference(
      identifier = identifier,
      baseUrl = buildWorkspaceBaseUrl(identifier).toString()
    )
  }

  private fun buildWorkspaceBaseUrl(identifier: WorkspaceIdentifier): URI {
    val urlVariables = mapOf(
      "workspaceIdentifier" to identifier.hashString()
    )
    return UriComponentsBuilder.fromUriString(appConfig.workspaces.baseUrlTemplate)
      .buildAndExpand(urlVariables)
      .toUri()
  }

  private fun <T> CreateOrReplaceable<T>.createOrReplaceWithLog(vararg items: T): T {
    log.debug("Creating or replacing:\n${items.joinToString(separator = "\n---\n") { yamlMapper.writeValueAsString(it) }}")
    return createOrReplace(*items)
  }

  private fun buildCompanionSpringConfig(wsIdentifier: WorkspaceIdentifier): String {
    val requiresAuth = workspaceClientService.requiresAuthentication()
    val config = CompanionSpringConfig(
      jwkSetUrl = appConfig.workspaces.jwkUrl.takeIf { requiresAuth },
      jwtClaimIssuer = appConfig.instanceId.takeIf { requiresAuth },
      jwtClaimAudience = wsIdentifier.hashString().takeIf { requiresAuth }
    )
    return jsonMapper.writeValueAsString(config)
  }
}
