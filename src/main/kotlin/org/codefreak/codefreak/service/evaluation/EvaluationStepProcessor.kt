package org.codefreak.codefreak.service.evaluation

import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepResult
import org.codefreak.codefreak.entity.EvaluationStepStatus
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.evaluation.report.EvaluationReportFormatParser
import org.codefreak.codefreak.service.evaluation.report.EvaluationReportParsingException
import org.codefreak.codefreak.service.evaluation.report.FormatParserRegistry
import org.codefreak.codefreak.util.withTrailingSlash
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.stereotype.Component

@Component
class EvaluationStepProcessor : ItemProcessor<EvaluationStep, EvaluationStep?> {

  @Autowired
  @Qualifier("asyncTaskExecutor")
  private lateinit var taskExecutor: AsyncTaskExecutor

  @Autowired
  private lateinit var evaluationBackend: EvaluationBackend

  @Autowired
  private lateinit var formatParserRegistry: FormatParserRegistry

  @Autowired
  private lateinit var answerService: AnswerService

  @Autowired
  private lateinit var appConfig: AppConfiguration

  @Value("#{@config.evaluation.defaultTimeout}")
  private var defaultTimeout: Long = 0L

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun process(evaluationStep: EvaluationStep): EvaluationStep? {
    val answer = evaluationStep.evaluation.answer

    log.debug("Processing evaluation step ${evaluationStep.definition.key} of answer ${evaluationStep.evaluation.answer.id}")
    try {
      // fail early if parser does not exist
      val reportFormatParser = formatParserRegistry.getParser(evaluationStep.definition.report.format)
      val feedbackList = try {
        runEvaluationWithTimeout(reportFormatParser, evaluationStep)
      } catch (e: InterruptedException) {
        // happens if the application shuts down. In this case we will stop any further processing.
        // The evaluation will be re-run if the application restarts
        return null
      }

      // This is a simple fix to make the absolute paths contained in the feedback relative
      feedbackList.forEach { feedback ->
        feedback.fileContext?.let {
          it.path = it.path.removePrefix(appConfig.evaluation.imageWorkdir.withTrailingSlash())
        }
      }

      evaluationStep.addAllFeedback(feedbackList)
      // only check for explicitly "failed" feedback so we ignore the "skipped" ones
      if (evaluationStep.feedback.any { it.isFailed }) {
        evaluationStep.result = EvaluationStepResult.FAILED
      } else {
        evaluationStep.result = EvaluationStepResult.SUCCESS
      }
      evaluationStep.summary = reportFormatParser.summarize(feedbackList)
    } catch (e: EvaluationStepException) {
      evaluationStep.result = e.result
      evaluationStep.summary = e.message
      if (e.status != null) {
        evaluationStep.status = e.status
      }
    } catch (e: Exception) {
      log.error("Evaluation step ${evaluationStep.definition.key} of answer ${answer.id} failed unexpectedly:", e)
      evaluationStep.result = EvaluationStepResult.ERRORED
      evaluationStep.summary = e.message ?: "Unknown error"
    }

    log.debug(
      "Evaluation step ${evaluationStep.definition.key} of answer ${answer.id} finished with status ${evaluationStep.result}: ${evaluationStep.summary}"
    )
    return evaluationStep
  }

  private fun runEvaluationWithTimeout(
    reportFormatParser: EvaluationReportFormatParser,
    step: EvaluationStep
  ): List<Feedback> {
    val answer = step.evaluation.answer
    val timeout = step.definition.timeout ?: defaultTimeout
    try {
      val task = taskExecutor.submit(Callable {
        runEvaluation(reportFormatParser, step)
      })
      return if (timeout > 0) {
        log.debug("Running evaluation step ${step.definition.key} of answer ${answer.id} with a time limit of $timeout seconds")
        task.get(timeout, TimeUnit.SECONDS)
      } else {
        log.info("Running evaluation step ${step.definition.key} of answer ${answer.id} WITHOUT time limit!")
        task.get()
      }
    } catch (e: TimeoutException) {
      log.info("Timeout for evaluation step ${step.definition.key} of answer ${answer.id} occurred after ${timeout}sec")
      evaluationBackend.interruptEvaluation(step.id)
      throw EvaluationStepException(
        "Evaluation timed out after $timeout seconds",
        result = EvaluationStepResult.ERRORED,
        status = EvaluationStepStatus.CANCELED
      )
    } catch (e: ExecutionException) {
      // unwrap exceptions from EvaluationRunner#run() to catch them properly
      throw e.cause ?: e
    }
  }

  private fun runEvaluation(reportFormatParser: EvaluationReportFormatParser, step: EvaluationStep): List<Feedback> {
    val definition = step.definition
    val config = buildEvaluationRunConfig(step)
    // file stream should be closed by evaluation backend as soon as files have been copied over
    return evaluationBackend.runEvaluation(config) { evaluationResult ->
      // a blank pattern indicates we should parse the process output instead of any files
      // we could introduce a more explicit flag or value for this like ":stdout"
      if (definition.report.path.isBlank()) {
        log.debug("No report file matching pattern given. Trying to extract feedback from stdout")
        reportFormatParser.parse(evaluationResult.output)
      } else {
        log.debug("Trying to generate feedback from files matching ${definition.report.path}")
        parseFeedbackFromEvaluationFiles(reportFormatParser, evaluationResult, definition.report.path)
      }
    }
  }

  private fun parseFeedbackFromEvaluationFiles(
    reportFormatParser: EvaluationReportFormatParser,
    evaluationResult: EvaluationResult,
    filePattern: String
  ): List<Feedback> {
    // a map with fileName to extracted feedback from this file
    // Map<String, List<Feedback>>
    val feedbackFromFiles = evaluationResult.consumeFiles(filePattern) { fileName, fileContent ->
      log.debug("Parsing file $fileName with ${reportFormatParser.id}")
      try {
        Pair(fileName, reportFormatParser.parse(fileContent))
      } catch (parsingException: EvaluationReportParsingException) {
        throw EvaluationStepException(
          "Failed to parse report file $fileName with ${reportFormatParser.id}: ${parsingException.message}",
          EvaluationStepResult.ERRORED,
          cause = parsingException
        )
      }
    }.toMap()

    // handle cases where no file was found
    if (feedbackFromFiles.isEmpty()) {
      return handleNoFilesFound(evaluationResult)
    }

    return feedbackFromFiles.values.flatten()
  }

  /**
   * This method will be called in case no matching files to parse have been found.
   * This could either be good or bad: E.g. in case no JUnit test reports have been found when using Java
   * this means compilation failed. In this cases the stdout gives more information about what went wrong.
   */
  fun handleNoFilesFound(evaluationResult: EvaluationResult): List<Feedback> {
    val exitCode = evaluationResult.exitCode
    val output = evaluationResult.output
    if (exitCode > 0) {
      throw EvaluationStepException(
        "Process exited with $exitCode:\n$output",
        EvaluationStepResult.FAILED,
        EvaluationStepStatus.FINISHED
      )
    } else {
      throw EvaluationStepException(
        "Process exited with $exitCode:\n$output",
        EvaluationStepResult.SUCCESS,
        EvaluationStepStatus.FINISHED
      )
    }
  }

  private fun buildEvaluationRunConfig(step: EvaluationStep): DefaultEvaluationRunConfig {
    val stepDefinition = step.definition
    return DefaultEvaluationRunConfig(
      id = step.id,
      imageName = appConfig.evaluation.defaultImage,
      workingDirectory = appConfig.evaluation.imageWorkdir,
      script = createEvaluationScript(stepDefinition.script),
      environment = buildEnvVariables(step),
      collectionId = step.evaluation.answer.id
    )
  }

  private fun buildEnvVariables(step: EvaluationStep): Map<String, String> {
    val answer = step.evaluation.answer
    val submission = answer.submission
    val user = submission.user
    return mapOf(
      "CI" to "true",
      "CODEFREAK_USER_USERNAME" to user.usernameCanonical,
      "CODEFREAK_USER_FIRST_NAME" to (user.firstName ?: ""),
      "CODEFREAK_USER_LAST_NAME" to (user.lastName ?: ""),
      "CODEFREAK_USER_ID" to user.id.toString(),
      "CODEFREAK_ANSWER_ID" to answer.id.toString(),
      "CODEFREAK_TASK_ID" to answer.task.id.toString(),
      "CODEFREAK_SUBMISSION_ID" to submission.id.toString(),
      "CODEFREAK_ASSIGNMENT_ID" to (submission.assignment?.id?.toString() ?: "")
    )
  }

  private fun createEvaluationScript(script: String): String {
    // If the script already contains a shebang line return it as-is.
    // The script might not be written in bash but another language
    if (script.startsWith("#!")) {
      return script
    }
    // Otherwise, create a proper bash script based on the given input
    // set -e            makes the script stop on a failing command
    // set -o pipefail   makes the script stop on failing pipes
    return "#!/usr/bin/env bash\n\nset -eo pipefail\n$script"
  }

  /**
   * Evaluation run config which invokes a supplied function to read files.
   */
  class DefaultEvaluationRunConfig(
    override val id: UUID,
    override val script: String,
    override val environment: Map<String, String>,
    override val imageName: String,
    override val workingDirectory: String,
    override val collectionId: UUID
  ) : EvaluationRunConfig
}
