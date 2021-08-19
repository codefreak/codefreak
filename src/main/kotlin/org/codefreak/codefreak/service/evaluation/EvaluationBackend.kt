package org.codefreak.codefreak.service.evaluation

import java.io.InputStream
import java.util.UUID

interface EvaluationResult {
  val exitCode: Int
  val output: String
  fun <T> consumeFiles(pattern: String, consumer: (fileName: String, fileContent: InputStream) -> T): List<T>
}

typealias EvaluationResultProcessor<T> = (result: EvaluationResult) -> T

interface EvaluationBackend {
  data class EvaluationRunConfig(
    val id: UUID,
    val script: String,
    val environment: Map<String, String>,
    val image: String,
    val workingDirectory: String,
    val filesSupplier: () -> InputStream
  ) {
    fun <T> useFiles(consumer: (input: InputStream) -> T): T = filesSupplier().use(consumer)
  }

  fun <T> runEvaluation(runConfig: EvaluationRunConfig, processResult: EvaluationResultProcessor<T>): T
  fun interruptEvaluation(id: UUID)
}
