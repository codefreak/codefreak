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
  private lateinit var workspaceService: WorkspaceService<KubernetesWorkspaceConfig>

  @Autowired
  private lateinit var workspaceClientFactory: WorkspaceClientFactory

  @Autowired
  private lateinit var appConfiguration: AppConfiguration

  override fun <T> runEvaluation(runConfig: EvaluationRunConfig, resultProcessor: EvaluationResultProcessor<T>): T {
    val workspaceConfig = createWorkspaceConfig(runConfig)
    return workspaceService.useWorkspace(workspaceConfig) { workspaceReference ->
      val result = runBlocking { invokeEvaluation(workspaceReference) }
      resultProcessor(result)
    }
  }

  private suspend fun invokeEvaluation(reference: WorkspaceReference): EvaluationResult {
    val workspaceClient = workspaceClientFactory.createClient(reference)
    val evalProcessId = workspaceClient.startProcess(listOf("/scripts/run-evaluation"))
    val exitCode = workspaceClient.waitForProcess(evalProcessId)
    val output = workspaceClient.getProcessOutput(evalProcessId).awaitLast()

    return object : EvaluationResult {
      override val exitCode = exitCode
      override val output = output

      override fun <T> consumeFiles(pattern: String, consumer: (fileName: String, fileContent: InputStream) -> T): List<T> {
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
        createMinimalWorkspaceConfig(id)
    )
  }

  private fun createMinimalWorkspaceConfig(id: UUID): KubernetesWorkspaceConfig {
    return KubernetesWorkspaceConfig(
        appConfig = appConfiguration,
        reference = id
    ) { throw IllegalStateException("This config is read-only") }
  }

  private fun createWorkspaceConfig(runConfig: EvaluationRunConfig): KubernetesWorkspaceConfig {
    return KubernetesWorkspaceConfig(
        appConfig = appConfiguration,
        reference = runConfig.id
        // imageName = runConfig.imageName
    ) { runConfig.files }.also {
      it.addScript("run-evaluation", runConfig.script)
    }
  }

  private fun <S, T : WorkspaceConfig> WorkspaceService<T>.useWorkspace(config: T, consumer: (config: WorkspaceReference) -> S): S {
    try {
      val reference = createWorkspace(config)
      return consumer(reference)
    } finally {
      deleteWorkspace(config)
    }
  }
}
