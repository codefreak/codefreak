package org.codefreak.codefreak.service.evaluation

import java.io.InputStream
import java.util.UUID
import java.util.stream.Collectors
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.runBlocking
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.service.workspace.RemoteWorkspaceReference
import org.codefreak.codefreak.service.workspace.WorkspaceClientService
import org.codefreak.codefreak.service.workspace.WorkspaceConfiguration
import org.codefreak.codefreak.service.workspace.WorkspaceIdentifier
import org.codefreak.codefreak.service.workspace.WorkspacePurpose
import org.codefreak.codefreak.service.workspace.WorkspaceService
import org.codefreak.codefreak.util.TarUtil.entrySequence
import org.codefreak.codefreak.util.preventClose
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Name of the file that will be containing the evaluation-script.
 * Currently, this script is mounted at /scripts.
 */
const val EVALUATION_SCRIPT_NAME = "run-evaluation"

/**
 * Evaluation backend that creates a workspace for evaluating code.
 */
@Component
@ConditionalOnProperty("codefreak.evaluation.backend", havingValue = "workspace")
class WorkspaceEvaluationBackend : EvaluationBackend {
  @Autowired
  private lateinit var workspaceService: WorkspaceService

  @Autowired
  private lateinit var workspaceClientService: WorkspaceClientService

  @Autowired
  private lateinit var appConfiguration: AppConfiguration

  override fun <T> runEvaluation(runConfig: EvaluationRunConfig, resultProcessor: EvaluationResultProcessor<T>): T {
    val identifier = createEvaluationWorkspaceIdentifier(runConfig.id)
    try {
      val reference = workspaceService.createWorkspace(identifier, createEvaluationWorkspaceConfig(runConfig))
      val result = runBlocking { invokeEvaluation(reference) }
      return resultProcessor(result)
    } finally {
      workspaceService.deleteWorkspace(identifier)
    }
  }

  private suspend fun invokeEvaluation(reference: RemoteWorkspaceReference): EvaluationResult {
    val workspaceClient = workspaceClientService.createClient(reference)
    val evalProcessId = workspaceClient.startProcess(listOf("/scripts/$EVALUATION_SCRIPT_NAME"))
    val output = try {
      workspaceClient.getProcessOutput(evalProcessId).collect(Collectors.joining()).awaitLast()
    } catch (e: NoSuchElementException) {
      ""
    }
    val exitCode = workspaceClient.waitForProcess(evalProcessId)

    return object : EvaluationResult {
      override val exitCode = exitCode
      override val output = output

      override fun <T> consumeFiles(
        pattern: String,
        consumer: (fileName: String, fileContent: InputStream) -> T
      ): List<T> {
        return workspaceClient.downloadTar(filter = pattern) { rawTarArchive ->
          TarArchiveInputStream(rawTarArchive).use { tarArchive ->
            tarArchive.entrySequence()
              .map { consumer(it.name, tarArchive.preventClose()) }
              .toList()
          }
        }
      }
    }
  }

  override fun interruptEvaluation(id: UUID) {
    workspaceService.deleteWorkspace(
      createEvaluationWorkspaceIdentifier(id)
    )
  }

  private fun createEvaluationWorkspaceIdentifier(evaluationId: UUID): WorkspaceIdentifier {
    return WorkspaceIdentifier(
      purpose = WorkspacePurpose.EVALUATION,
      reference = evaluationId.toString()
    )
  }

  private fun createEvaluationWorkspaceConfig(runConfig: EvaluationRunConfig): WorkspaceConfiguration {
    return WorkspaceConfiguration(
      // TODO: Use image that might has been configured by the eval config
      imageName = appConfiguration.workspaces.companionImage,
      collectionId = runConfig.collectionId,
      isReadOnly = true,
      scripts = mapOf(
        EVALUATION_SCRIPT_NAME to runConfig.script
      ),
      environment = runConfig.environment
    )
  }
}
