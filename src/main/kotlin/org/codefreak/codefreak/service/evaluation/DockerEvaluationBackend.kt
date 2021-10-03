package org.codefreak.codefreak.service.evaluation

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.exceptions.ContainerNotFoundException
import com.spotify.docker.client.exceptions.DockerException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.service.ContainerService
import org.codefreak.codefreak.service.ExecResult
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.TarUtil.entrySequence
import org.codefreak.codefreak.util.preventClose
import org.codefreak.codefreak.util.withTrailingSlash
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.util.StreamUtils

@Component
@ConditionalOnProperty("codefreak.evaluation.backend", havingValue = "docker")
class DockerEvaluationBackend : EvaluationBackend {

  @Autowired
  private lateinit var appConfiguration: AppConfiguration

  @Autowired
  private lateinit var containerService: ContainerService

  @Autowired
  private lateinit var fileService: FileService

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun <T> runEvaluation(runConfig: EvaluationRunConfig, resultProcessor: EvaluationResultProcessor<T>): T {
    val id = runConfig.id
    val containerId = containerService.createContainer(runConfig.imageName) {
      // the unique name will make the evaluation fail if it is already running
      name = buildContainerName(id)
      doNothingAndKeepAlive()
      containerConfig {
        workingDir(runConfig.workingDirectory)
        env(runConfig.environment.entries.map { (k, v) -> "$k=$v" })
      }
      labels = labels + getEvalContainerListMap(id)
    }
    return containerService.useContainer(containerId) {
      // copy over project files that will be evaluated
      fileService.readCollectionTar(runConfig.collectionId).use {
        containerService.copyToContainer(it, containerId, runConfig.workingDirectory)
      }
      // copy all scripts
      containerService.copyToContainer(
          buildScriptsTarArchive(
              mapOf(
                  "/scripts/run-evaluation" to runConfig.script
              )
          ), containerId, "/"
      )
      val result = containerService.exec(containerId, arrayOf("/scripts/run-evaluation"))
      // TODO: At this point we should check if the evaluation has been interrupted from outside.
      // This could be done checking the exit code of the process. 128 + SIGNAL represents the exit code in bash when
      // execution has been interrupted. But this might also be the case if the container has been killed by the
      // system's watch dog.
      resultProcessor(createEvaluationResult(containerId, result, runConfig))
    }
  }

  private fun buildContainerName(id: UUID): String = "cf-${appConfiguration.instanceId}-eval-$id"

  override fun interruptEvaluation(id: UUID) {
    val containerName = buildContainerName(id)
    try {
      containerService.removeContainer(containerName, force = true, removeVolumes = true)
      log.debug("Removed evaluation container $containerName forcibly.")
    } catch (e: ContainerNotFoundException) {
      log.debug("Cannot find any evaluation container for evaluation step $id")
    } catch (e: DockerException) {
      log.warn("Cannot remove evaluation container $containerName!", e)
    }
  }

  /**
   * Create an evaluation result based on the exec result that gives access to the files inside the container using
   * the container service.
   */
  private fun createEvaluationResult(containerId: String, execResult: ExecResult, runConfig: EvaluationRunConfig) = object : EvaluationResult {
    override val exitCode: Int = execResult.exitCode.toInt()
    override val output: String = execResult.output
    override fun <T> consumeFiles(pattern: String, consumer: (fileName: String, fileContent: InputStream) -> T): List<T> {
      // we can only specify a path when getting an archive from the docker container.
      // Thus, we have to fetch all files and filter the files while iterating the archive.
      val matcher = AntPathMatcher()
      return containerService.archiveContainer(
          containerId,
          "${runConfig.workingDirectory.withTrailingSlash()}."
      ) { archiveStream ->
        TarArchiveInputStream(archiveStream).use { tarStream ->
          tarStream.entrySequence()
              .filter { matcher.match(TarUtil.normalizeFileName(pattern), TarUtil.normalizeFileName(it.name)) }
              .map {
                // we are working with the underlying http stream which should not be closed outside this method
                consumer(it.name, tarStream.preventClose())
              }
              .toList()
        }
      }
    }
  }

  private fun getEvalContainerListParams(id: UUID): Array<DockerClient.ListContainersParam> {
    return getEvalContainerListMap(id).map { (k, v) -> DockerClient.ListContainersParam.withLabel(k, v) }.toTypedArray()
  }

  private fun getEvalContainerListMap(id: UUID): Map<String, String> {
    return mapOf(ContainerService.LABEL_PREFIX + "eval.step-id" to id.toString())
  }

  /**
   * Create a tar archive that contains a single script entry.
   * The archive will be extracted to the evaluation container
   */
  private fun buildScriptsTarArchive(scripts: Map<String, String>): InputStream {
    val outputStream = ByteArrayOutputStream()
    TarUtil.PosixTarArchiveOutputStream(outputStream).use { archive ->
      scripts.map { (name, content) ->
        val entry = TarArchiveEntry(name)
        entry.size = content.encodeToByteArray().size.toLong()
        entry.mode = 493 // 755 = rwxr-xr-x
        archive.putArchiveEntry(entry)
        StreamUtils.copy(content, Charsets.UTF_8, archive)
        archive.closeArchiveEntry()
      }
      archive.finish()
    }
    return ByteArrayInputStream(outputStream.toByteArray())
  }
}
