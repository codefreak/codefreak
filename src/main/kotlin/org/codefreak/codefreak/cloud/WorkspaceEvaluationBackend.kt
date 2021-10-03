package org.codefreak.codefreak.cloud

import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.runBlocking
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.service.evaluation.EvaluationBackend
import org.codefreak.codefreak.service.evaluation.EvaluationResult
import org.codefreak.codefreak.service.evaluation.EvaluationResultProcessor
import org.codefreak.codefreak.service.evaluation.EvaluationRunConfig
import org.codefreak.codefreak.util.TarUtil.entrySequence
import org.codefreak.codefreak.util.preventClose
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("codefreak.evaluation.backend", havingValue = "workspace", matchIfMissing = true)
class WorkspaceEvaluationBackend : EvaluationBackend {
  @Autowired
  private lateinit var workspaceService: WorkspaceService

  @Autowired
  private lateinit var workspaceClientFactory: WorkspaceClientFactory

  @Autowired
  private lateinit var appConfiguration: AppConfiguration

  override fun <T> runEvaluation(runConfig: EvaluationRunConfig, resultProcessor: EvaluationResultProcessor<T>): T {
    val identifier = createWorkspaceIdentifier(runConfig.id)
    try {
      val reference = workspaceService.createWorkspace(identifier, createWorkspaceConfig(runConfig))
      val result = runBlocking { invokeEvaluation(reference) }
      return resultProcessor(result)
    } finally {
      workspaceService.deleteWorkspace(identifier)
    }
  }

  private suspend fun invokeEvaluation(reference: RemoteWorkspaceReference): EvaluationResult {
    val workspaceClient = workspaceClientFactory.createClient(reference)
    val evalProcessId = workspaceClient.startProcess(listOf("/scripts/run-evaluation"))
    val exitCode = workspaceClient.waitForProcess(evalProcessId)
    val output = workspaceClient.getProcessOutput(evalProcessId).awaitLast()

    return object : EvaluationResult {
      override val exitCode = exitCode
      override val output = output

      override fun <T> consumeFiles(
        pattern: String,
        consumer: (fileName: String, fileContent: InputStream) -> T
      ): List<T> {
        return workspaceClient.downloadFiles(filter = pattern) { downloadedTarArchive ->
          downloadedTarArchive.entrySequence()
            .map { consumer(it.name, downloadedTarArchive.preventClose()) }
            .toList()
        }
      }
    }
  }

  override fun interruptEvaluation(id: UUID) {
    workspaceService.deleteWorkspace(
      createWorkspaceIdentifier(id)
    )
  }

  private fun createWorkspaceIdentifier(id: UUID): WorkspaceIdentifier {
    return WorkspaceIdentifier(
      purpose = WorkspacePurpose.EVALUATION,
      reference = id.toString()
    )
  }

  private fun createWorkspaceConfig(runConfig: EvaluationRunConfig): WorkspaceConfiguration {
    return DefaultWorkspaceConfiguration(
      reference = runConfig.id,
      collectionId = runConfig.collectionId,
      isReadOnly = true,
      // TODO: Use image that might has been configured by the eval config
      imageName = appConfiguration.workspaces.companionImage
    ).also {
      it.addScript("run-evaluation", runConfig.script)
    }
  }
}
