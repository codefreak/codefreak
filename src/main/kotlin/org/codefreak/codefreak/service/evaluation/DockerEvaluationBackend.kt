package org.codefreak.codefreak.service.evaluation

import com.spotify.docker.client.DockerClient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.codefreak.codefreak.service.ContainerService
import org.codefreak.codefreak.service.ExecResult
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.TarUtil.entrySequence
import org.codefreak.codefreak.util.preventClose
import org.codefreak.codefreak.util.withTrailingSlash
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.util.StreamUtils

@Component
class DockerEvaluationBackend : EvaluationBackend {

  @Autowired
  private lateinit var containerService: ContainerService

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun <T> runEvaluation(runConfig: EvaluationBackend.EvaluationRunConfig, processResult: EvaluationResultProcessor<T>): T {
    val id = runConfig.id
    val containerId = containerService.createContainer(runConfig.image) {
      doNothingAndKeepAlive()
      containerConfig {
        workingDir(runConfig.projectPath)
        env(runConfig.environment.entries.map { (k, v) -> "$k=$v" })
      }
      labels = labels + getEvalContainerListMap(id)
    }
    return containerService.useContainer(containerId) {
      // copy over project files that will be evaluated
      runConfig.useFiles {
        containerService.copyToContainer(it, containerId, runConfig.projectPath)
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
      processResult(createEvaluationResult(containerId, result, runConfig))
    }
  }

  override fun interruptEvaluation(id: UUID) {
    val containerId = containerService.listContainers(*getEvalContainerListParams(id)).firstOrNull()?.id()
    if (containerId == null) {
      // container has already exited
      log.debug("Cannot find any evaluation container for evaluation step $id")
      return
    }
    log.debug("Removing container $containerId running step $id")
    containerService.removeContainer(containerId, force = true, removeVolumes = true)
  }

  /**
   * Create an evaluation result based on the exec result that gives access to the files inside the container using
   * the container service.
   */
  private fun createEvaluationResult(containerId: String, execResult: ExecResult, runConfig: EvaluationBackend.EvaluationRunConfig) = object : EvaluationResult {
    override val exitCode: Int = execResult.exitCode.toInt()
    override val output: String = execResult.output
    override fun <T> consumeFiles(pattern: String, consumer: (fileName: String, fileContent: InputStream) -> T): List<T> {
      // we can only specify a path when getting an archive from the docker container.
      // Thus, we have to fetch all files and filter the files while iterating the archive.
      val matcher = AntPathMatcher()
      return containerService.archiveContainer(containerId, "${runConfig.projectPath.withTrailingSlash()}.") { archiveStream ->
        TarArchiveInputStream(archiveStream).use { tarStream ->
          tarStream.entrySequence()
              .filter { matcher.match(pattern, it.name) }
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
