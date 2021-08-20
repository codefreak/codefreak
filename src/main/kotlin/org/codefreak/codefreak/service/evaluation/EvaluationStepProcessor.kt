package org.codefreak.codefreak.service.evaluation

import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.codefreak.codefreak.cloud.WorkspaceClient
import org.codefreak.codefreak.cloud.WorkspaceClientFactory
import org.codefreak.codefreak.cloud.WorkspaceService
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepResult
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.TarUtil.entrySequence
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EvaluationStepProcessor : ItemProcessor<EvaluationStep, EvaluationStep?> {

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  private lateinit var workspaceClientFactory: WorkspaceClientFactory

  @Autowired
  private lateinit var workspaceService: WorkspaceService

  @Value("#{@config.evaluation.defaultTimeout}")
  private var defaultTimeout: Long = 0L

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var junitXmlFormatParser: JunitXmlFormatParser

  override fun process(evaluationStep: EvaluationStep): EvaluationStep? {
    val answer = evaluationStep.evaluation.answer
    log.debug("Start evaluation of answer {} ({} steps)", answer.id, answer.task.evaluationStepDefinitions.size)

    val stepDefinition = evaluationStep.definition
    val runnerName = stepDefinition.runnerName
    val workspaceConfig = workspaceService.createWorkspaceConfigForCollection(evaluationStep.id).also {
      it.addScript(
          "run-evaluation", """
        #!/bin/env bash
        apt-get update
        apt-get install -y unzip
        curl -o /tmp/gradle-7.1.1-bin.zip -L "https://services.gradle.org/distributions/gradle-7.1.1-bin.zip"
        unzip -d /opt/gradle /tmp/gradle-7.1.1-bin.zip
        /opt/gradle/gradle-7.1.1/bin/gradle testClasses
        /opt/gradle/gradle-7.1.1/bin/gradle test
      """.trimIndent()
      )
    }
    try {
      val workspace = workspaceService.createWorkspace(workspaceConfig)
      val workspaceClient = workspaceClientFactory.createClient(workspace)
      val feedbackList = try {
        runEvaluation(workspaceClient, evaluationStep)
      } catch (e: InterruptedException) {
        // happens if the application shuts down. In this case we will stop any further processing.
        // The evaluation will be re-run if the application restarts
        return null
      }
      evaluationStep.addAllFeedback(feedbackList)
      // only check for explicitly "failed" feedback so we ignore the "skipped" ones
      if (evaluationStep.feedback.any { feedback -> feedback.isFailed }) {
        evaluationStep.result = EvaluationStepResult.FAILED
      } else {
        evaluationStep.result = EvaluationStepResult.SUCCESS
      }
    } catch (e: EvaluationStepException) {
      evaluationStep.result = e.result
      evaluationStep.summary = e.message
      if (e.status != null) {
        evaluationStep.status = e.status
      }
    } catch (e: Exception) {
      log.error("Evaluation step $runnerName of answer ${answer.id} failed unexpectedly:", e)
      evaluationStep.result = EvaluationStepResult.ERRORED
      evaluationStep.summary = e.message ?: "Unknown error"
    } finally {
      workspaceService.deleteWorkspace(workspaceConfig)
    }

    log.debug(
        "Step $runnerName finished ${evaluationStep.result}: ${evaluationStep.summary}"
    )
    return evaluationStep
  }

  private fun runEvaluation(workspaceClient: WorkspaceClient, evaluationStep: EvaluationStep): List<Feedback> {
    return runBlocking {
      println("waiting for workspace to come live")
      val start = Instant.now().epochSecond
      while (true) {
        if (workspaceClient.isWorkspaceLive()) {
          val duration = Instant.now().epochSecond - start
          println("Workspace is live after ${duration}sec!")
          break
        } else {
          delay(250)
        }
      }

      fileService.readCollectionTar(evaluationStep.evaluation.answer.id).use { collectionStream ->
        workspaceClient.deployFiles(collectionStream)
      }
      log.debug("Starting evaluation process of step ${evaluationStep.id}")
      val processId = workspaceClient.startProcess(listOf("/bin/bash", "/scripts/run-evaluation"))
      val processOutput = StringBuilder()
      val outputStream = workspaceClient.getProcessOutput(processId).subscribe {
        println(it)
        processOutput.append(it)
      }
      log.debug("Waiting for evaluation process of step ${evaluationStep.id} to finish...")
      val exitCode = workspaceClient.waitForProcess(processId)
      log.debug("Process of evaluation ${evaluationStep.id} exited with $exitCode")
      outputStream.dispose()
      val feedback = workspaceClient.downloadFiles(filter = "build/test-results/test/*.xml") { archive ->
        archive.entrySequence()
            .filter { it.isFile }
            .flatMap {
          log.debug("Parsing file ${it.name} from evaluation step ${evaluationStep.id}")
          junitXmlFormatParser.parse(archive.decodeToString())
        }.toList()
      }
      if (feedback.isEmpty() && exitCode > 0) {
        throw EvaluationStepException(
            processOutput.toString(),
            EvaluationStepResult.ERRORED
        )
      }
      feedback
    }
  }

  fun TarArchiveInputStream.decodeToString(): String {
    return readBytes().decodeToString()
  }
}
