package org.codefreak.codefreak.service.evaluation

import java.io.InputStream
import java.util.UUID

/**
 * An evaluation backend is a system which will execute students code in an (isolated) environment.
 * This could be a Docker Container, Kubernetes Container or other executing environments like virtual machines
 * or local system. Each evaluation step has a unique id which currently corresponds to the evaluation step id
 * but implementation should not rely on this behavior.
 */
interface EvaluationBackend {
  /**
   * Run the evaluation based on the supplied config and invoke the result processor after the evaluation has been run.
   */
  fun <T> runEvaluation(runConfig: EvaluationRunConfig, resultProcessor: EvaluationResultProcessor<T>): T

  /**
   * Terminate a running evaluation with the given ID. It should be ignored if the evaluation is not running (anymore).
   */
  fun interruptEvaluation(id: UUID)
}

/**
 * Interface to collect information about a finished evaluation.
 */
interface EvaluationResult {
  /**
   * The final exit code of the process
   */
  val exitCode: Int

  /**
   * The combined stdout/stderr output of the process
   */
  val output: String

  /**
   * Consume files that might have been generated during evaluation that match the given Ant-style path pattern.
   * The consumer function should be invoked for each matching file (excluding directories).
   * The returned list is a collection of the results returned from the consumer function.
   */
  fun <T> consumeFiles(pattern: String, consumer: (fileName: String, fileContent: InputStream) -> T): List<T>
}

typealias EvaluationResultProcessor<T> = (result: EvaluationResult) -> T

/**
 * Configuration each evaluation is based on.
 */
interface EvaluationRunConfig {
  /**
   * The unique identified for each running evaluation.
   * Currently, this is the id of the evaluation step, but this might change in the future.
   */
  val id: UUID

  /**
   * The content of a (bash) script that should be invoked for evaluation.
   */
  val script: String

  /**
   * A map of environment variables which should be set during evaluation for the script.
   */
  val environment: Map<String, String>

  /**
   * The full name of a Docker image which should be used for evaluation.
   */
  val imageName: String

  /**
   * The name of a directory which should be the working directory when executing the evaluation script.
   */
  val workingDirectory: String

  val collectionId: UUID
}
