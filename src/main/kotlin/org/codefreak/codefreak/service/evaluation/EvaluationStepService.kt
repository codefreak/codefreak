package org.codefreak.codefreak.service.evaluation

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant
import java.util.UUID
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.codefreak.codefreak.entity.EvaluationStepResult
import org.codefreak.codefreak.entity.EvaluationStepStatus
import org.codefreak.codefreak.repository.EvaluationStepRepository
import org.codefreak.codefreak.service.EntityNotFoundException
import org.codefreak.codefreak.service.EvaluationStatusUpdatedEvent
import org.codefreak.codefreak.service.EvaluationStepStatusUpdatedEvent
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.TaskUtil.isHidden
import org.codefreak.codefreak.util.TaskUtil.isProtected
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EvaluationStepService {

  @Autowired
  private lateinit var eventPublisher: ApplicationEventPublisher

  @Autowired
  private lateinit var stepRepository: EvaluationStepRepository

  @Autowired
  private lateinit var fileService: FileService

  fun getEvaluationStep(stepId: UUID): EvaluationStep {
    return stepRepository.findById(stepId).orElseThrow {
      EntityNotFoundException("EvaluationStep $stepId could not be found")
    }
  }

  /**
   * Determine if an evaluation step has to be executed by a runner
   */
  fun stepNeedsExecution(step: EvaluationStep): Boolean {
    // non-finished steps or errored steps can be executed again
    return step.status != EvaluationStepStatus.FINISHED || step.result === EvaluationStepResult.ERRORED
  }

  @Transactional
  fun updateEvaluationStepStatus(stepId: UUID, status: EvaluationStepStatus) {
    updateEvaluationStepStatus(getEvaluationStep(stepId), status)
  }

  @Transactional
  fun updateEvaluationStepStatus(step: EvaluationStep, status: EvaluationStepStatus) {
    // We do not check if the step is already in the given status on purpose.
    // This allows updating the finishedAt timestamp.
    when {
      status == EvaluationStepStatus.QUEUED -> {
        step.queuedAt = Instant.now()
        step.finishedAt = null
      }
      status >= EvaluationStepStatus.FINISHED -> step.finishedAt = Instant.now()
    }
    val evaluation = step.evaluation
    val originalEvaluationStatus = evaluation.stepStatusSummary
    step.status = status
    val newEvaluationStatus = evaluation.stepStatusSummary
    eventPublisher.publishEvent(EvaluationStepStatusUpdatedEvent(step, status))
    // check if status of evaluation has changed
    if (originalEvaluationStatus !== newEvaluationStatus) {
      eventPublisher.publishEvent(EvaluationStatusUpdatedEvent(evaluation, newEvaluationStatus))
    }
  }

  /**
   * Add a new evaluation step to the evaluation based on a given definition.
   * If a step with the same definition already exists, it will be replaced.
   */
  fun addStepToEvaluation(evaluation: Evaluation, stepDefinition: EvaluationStepDefinition): EvaluationStep {
    // remove existing step with this definition from evaluation
    evaluation.evaluationSteps.removeIf { it.definition == stepDefinition }
    return EvaluationStep(stepDefinition, evaluation, EvaluationStepStatus.PENDING).also {
      evaluation.addStep(it)
    }
  }

  fun saveEvaluationStep(step: EvaluationStep) {
    stepRepository.save(step)
  }

  /**
   * Get the files needed for running the given evaluation step. The returned archive will contain
   * the files from the answer overlapped by protected and hidden files.
   */
  fun getFilesForEvaluation(evaluationStepId: UUID): InputStream {
    val evaluationStep = try {
      getEvaluationStep(evaluationStepId)
    } catch (e: EntityNotFoundException) {
      throw IllegalStateException("Cannot load files of evaluation step $evaluationStepId because it does not exist in the database")
    }
    return copyFilesForEvaluation(evaluationStep.evaluation.answer)
  }

  /**
   * Creates an in-memory tar archive containing the files from the answer overlapped by hidden and protected files.
   */
  private fun copyFilesForEvaluation(answer: Answer): InputStream {
    val out = ByteArrayOutputStream()
    val outTar = TarUtil.PosixTarArchiveOutputStream(out)
    fileService.readCollectionTar(answer.id).use { answerFiles ->
      TarUtil.copyEntries(TarArchiveInputStream(answerFiles), outTar, filter = {
        !answer.task.isHidden(it) && !answer.task.isProtected(it)
      })
    }
    fileService.readCollectionTar(answer.task.id).use { taskFiles ->
      TarUtil.copyEntries(TarArchiveInputStream(taskFiles), outTar, filter = {
        answer.task.isHidden(it) || answer.task.isProtected(it)
      })
    }
    return ByteArrayInputStream(out.toByteArray())
  }
}
