package org.codefreak.codefreak.service

import com.google.common.collect.MapMaker
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.ListContainersParam.allContainers
import com.spotify.docker.client.DockerClient.ListContainersParam.withLabel
import com.spotify.docker.client.DockerClient.RemoveContainerParam.forceKill
import com.spotify.docker.client.DockerClient.RemoveContainerParam.removeVolumes
import com.spotify.docker.client.exceptions.ContainerNotFoundException
import com.spotify.docker.client.exceptions.ImageNotFoundException
import com.spotify.docker.client.messages.Container
import com.spotify.docker.client.messages.ContainerInfo
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import javax.ws.rs.ProcessingException
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.util.DockerUtil
import org.glassfish.jersey.internal.LocalizationMessages
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ContainerService : BaseService() {

  companion object {
    const val LABEL_PREFIX = "org.codefreak."
    const val LABEL_INSTANCE_ID = LABEL_PREFIX + "instance-id"
  }

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  lateinit var docker: DockerClient

  @Autowired
  lateinit var config: AppConfiguration

  /**
   * A map that keeps a lock for each answer (collection) that should be used for file operations
   * @see withCollectionFileLock
   */
  private val answerFileLockMap = MapMaker().weakValues().makeMap<UUID, ReentrantLock>()

  fun pullDockerImage(image: String) {
    val imageInfo = try {
      docker.inspectImage(image)
    } catch (e: ImageNotFoundException) {
      null
    }

    val pullRequired = config.docker.pullPolicy == "always" || (config.docker.pullPolicy == "if-not-present" && imageInfo == null)
    if (!pullRequired) {
      if (imageInfo == null) {
        log.warn("Image pulling is disabled but $image is not available on the daemon!")
      } else {
        log.debug("Image present: $image ${imageInfo.id()}")
      }
      return
    }

    log.info("Pulling latest image for: $image")
    docker.pull(image)
    log.info("Updated docker image $image to ${docker.inspectImage(image).id()}")
  }

  fun inspectContainer(containerId: String): ContainerInfo = docker.inspectContainer(containerId)

  fun startContainer(containerId: String) = docker.startContainer(containerId)

  fun stopContainer(containerId: String, secondsBeforeKilling: Int) = docker.stopContainer(
      containerId,
      secondsBeforeKilling
  )

  fun copyToContainer(tar: InputStream, containerId: String, path: String) {
    docker.copyToContainer(
        tar,
        containerId,
        path
    )
  }

  fun connectToNetwork(containerId: String, network: String) {
    docker.connectToNetwork(containerId, network)
  }

  fun removeContainer(containerId: String, force: Boolean = false, removeVolumes: Boolean = false) {
    docker.removeContainer(
        containerId,
        removeVolumes(removeVolumes),
        forceKill(force)
    )
  }

  /**
   * Run a command as root inside container and return the result as string
   */
  fun exec(containerId: String, cmd: Array<String>): ExecResult {
    val exec = docker.execCreate(
        containerId, cmd,
        DockerClient.ExecCreateParam.attachStdin(), // this is not needed but a workaround for spotify/docker-client#513
        DockerClient.ExecCreateParam.attachStdout(),
        DockerClient.ExecCreateParam.attachStderr(),
        DockerClient.ExecCreateParam.user("root")
    )
    return ExecResult(output = docker.execStart(exec.id()).readFully(),
        exitCode = docker.execInspect(exec.id()).exitCode() ?: -1)
  }

  fun getContainerWithLabel(label: String, value: String? = null): Container? {
    return getContainersWithLabel(label, value).firstOrNull()
  }

  protected fun getContainersWithLabel(label: String, value: String? = null) = listContainers(
      withLabel(label, value),
      allContainers()
  )

  fun listContainers(vararg listContainerParams: DockerClient.ListContainersParam): List<Container> {
    return docker.listContainers(withLabel(LABEL_INSTANCE_ID, config.instanceId), *listContainerParams)
  }

  fun archiveContainer(containerId: String, path: String, process: (InputStream) -> Unit) {
    try {
      docker.archiveContainer(containerId, path).use(process)
    } catch (e: ProcessingException) {
      // okay until this is fixed https://github.com/eclipse-ee4j/jersey/issues/3486
      if (e.message != LocalizationMessages.MESSAGE_CONTENT_INPUT_STREAM_CLOSE_FAILED()) {
        throw e
      }
    }
  }

  fun createContainer(
    image: String,
    configure: ContainerConfigurator
  ): String {
    val normalizedImageName = DockerUtil.normalizeImageName(image)
    pullDockerImage(normalizedImageName)

    val builder = ContainerBuilder()
    builder.configure()

    builder.containerConfig {
      image(normalizedImageName)
    }
    builder.labels += mapOf(
        LABEL_INSTANCE_ID to config.instanceId
    )

    return docker.createContainer(builder.build(), builder.name).id()!!
  }

  fun isContainerRunning(containerId: String): Boolean = try {
    docker.inspectContainer(containerId).state().running()
  } catch (e: ContainerNotFoundException) {
    false
  }

  /**
   * Start a container and runs the specified block.
   * Ensures container is removed after execution with all attached volumes
   */
  final inline fun <T> useContainer(containerId: String, block: () -> T): T {
    docker.startContainer(containerId)
    try {
      return block()
    } finally {
      docker.removeContainer(containerId, forceKill(), removeVolumes())
    }
  }

  /**
   * Lock collection for file operations (read/write files in the container)
   * We cannot use the container id for locking because we need to lock even before we know the container id
   */
  fun <T> withCollectionFileLock(collectionId: UUID, block: () -> T): T {
    val lock = answerFileLockMap.getOrPut(collectionId) { ReentrantLock() }
    lock.lock()
    try {
      return block()
    } finally {
      lock.unlock()
    }
  }
}
