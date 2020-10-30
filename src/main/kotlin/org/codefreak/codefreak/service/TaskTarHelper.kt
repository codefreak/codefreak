package org.codefreak.codefreak.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.utils.IOUtils
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.EvaluationStepDefinitionRepository
import org.codefreak.codefreak.service.evaluation.EvaluationService
import org.codefreak.codefreak.service.evaluation.isBuiltIn
import org.codefreak.codefreak.service.evaluation.runner.CommentRunner
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.TarUtil.getCodefreakDefinition
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
internal class TaskTarHelper {

  @Autowired
  @Qualifier("yamlObjectMapper")
  private lateinit var yamlMapper: ObjectMapper

  @Autowired
  private lateinit var taskService: TaskService

  @Autowired
  private lateinit var evaluationService: EvaluationService

  @Autowired
  private lateinit var evaluationStepDefinitionRepository: EvaluationStepDefinitionRepository

  @Autowired
  private lateinit var fileService: FileService

  fun createFromTar(tarContent: ByteArray, assignment: Assignment?, owner: User, position: Long): Task {
    val definition = yamlMapper.getCodefreakDefinition<TaskDefinition>(tarContent.inputStream())

    val existingTask = try {
      val taskId = UUID.fromString(definition.id ?: "")
      taskService.findTask(taskId).takeIf { it.owner == owner }
    } catch (e: IllegalArgumentException) {
      // Thrown if no valid id is found in the task definition
      null
    } catch (e: EntityNotFoundException) {
      // Thrown if task is not found in the repository
      null
    }

    var task = if (existingTask == null) {
      createNewTaskFromTar(definition, assignment, owner, position)
    } else {
      updateExistingTaskFromTar(existingTask, definition)
    }

    saveTaskEvaluationStepDefinitions(task)
    task = taskService.saveTask(task)
    copyTaskFilesFromTar(task.id, tarContent)

    return task
  }

  private fun updateExistingTaskFromTar(task: Task, definition: TaskDefinition): Task {
    val updatedAt = Instant.parse(definition.updatedAt)
    val isUpToDate = !updatedAt.isAfter(task.updatedAt)

    if (isUpToDate) {
      return task
    }

    var updatedTask = task

    updatedTask.title = definition.title
    updatedTask.body = definition.description
    updatedTask.hiddenFiles = definition.hidden
    updatedTask.protectedFiles = definition.protected
    updatedTask.ideEnabled = definition.ide?.enabled ?: true
    updatedTask.ideImage = definition.ide?.image

    updatedTask = taskService.saveTask(updatedTask)

    val stepDefinitions = getEvaluationStepDefinitions(definition, task)

    updateExistingEvaluationStepDefinitions(updatedTask, stepDefinitions)

    val newStepDefinitions = stepDefinitions.filter {
      updatedTask.evaluationStepDefinitions.find { evaluationStepDefinition -> it == evaluationStepDefinition } == null
    }
    updatedTask.evaluationStepDefinitions.addAll(newStepDefinitions)

    return updatedTask
  }

  private fun updateExistingEvaluationStepDefinitions(task: Task, newStepDefinitions: Collection<EvaluationStepDefinition>) {
    task.evaluationStepDefinitions.forEach { existingStepDefinition ->
      val stepDefinition = newStepDefinitions.find { it == existingStepDefinition }

      if (stepDefinition != null) {
        evaluationService.updateEvaluationStepDefinition(existingStepDefinition, stepDefinition.title, stepDefinition.active, stepDefinition.options)
      } else {
        try {
          evaluationService.deleteEvaluationStepDefinition(existingStepDefinition)
        } catch (e: DataIntegrityViolationException) {
          // The step cannot be deleted if it has been used for evaluation already
          // Deactivate it for future evaluation instead
          existingStepDefinition.active = false
        }
      }
    }
  }

  private fun createNewTaskFromTar(definition: TaskDefinition, assignment: Assignment?, owner: User, position: Long): Task {
    var task = Task(assignment, owner, position, definition.title, definition.description, 100)
    task.hiddenFiles = definition.hidden
    task.protectedFiles = definition.protected
    task.ideEnabled = definition.ide?.enabled ?: true
    task.ideImage = definition.ide?.image

    task = taskService.saveTask(task)
    task.evaluationStepDefinitions = getEvaluationStepDefinitions(definition, task)

    task.evaluationStepDefinitions
        .groupBy { it.runnerName }
        .forEach { (runnerName, definitions) ->
          if (definitions.size > 1 && evaluationService.getEvaluationRunner(runnerName).isBuiltIn()) {
            throw IllegalArgumentException("Evaluation step '$runnerName' can only be added once!")
          }
        }

    return task
  }

  private fun getEvaluationStepDefinitions(taskDefinition: TaskDefinition, task: Task) = taskDefinition.evaluation
      .mapIndexed { index, it ->
        val runner = evaluationService.getEvaluationRunner(it.step)
        val title = it.title ?: runner.getDefaultTitle()
        val stepDefinition = EvaluationStepDefinition(task, runner.getName(), index, title, it.options)
        stepDefinition.id = try {
          UUID.fromString(it.id ?: "")
        } catch (e: IllegalArgumentException) {
          stepDefinition.id
        }
        evaluationService.validateRunnerOptions(stepDefinition)
        stepDefinition
      }
      .toMutableSet()

  private fun copyTaskFilesFromTar(taskId: UUID, tarContent: ByteArray) {
    fileService.writeCollectionTar(taskId).use { fileCollection ->
      TarUtil.copyEntries(tarContent.inputStream(), fileCollection, filter = { !it.name.equals(TarUtil.CODEFREAK_DEFINITION_NAME, true) })
    }
  }

  private fun saveTaskEvaluationStepDefinitions(task: Task) {
    addBuiltInEvaluationSteps(task)
    evaluationStepDefinitionRepository.saveAll(task.evaluationStepDefinitions)
  }

  fun createMultipleFromTar(tarContent: ByteArray, assignment: Assignment?, owner: User, position: Long) {
    val input = TarArchiveInputStream(ByteArrayInputStream(tarContent))
    generateSequence { input.nextTarEntry }
        .filter { it.isFile }
        .filter { it.name.endsWith(".tar", ignoreCase = true).or(it.name.endsWith(".zip", ignoreCase = true)) }
        .forEach { _ ->
          createFromTar(IOUtils.toByteArray(input), assignment, owner, position)
        }
  }

  private fun addBuiltInEvaluationSteps(task: Task) {
    if (task.evaluationStepDefinitions.find { it.runnerName == CommentRunner.RUNNER_NAME } == null) {
      task.evaluationStepDefinitions.forEach { it.position++ }
      val runner = evaluationService.getEvaluationRunner(CommentRunner.RUNNER_NAME)
      task.evaluationStepDefinitions.add(EvaluationStepDefinition(task, runner.getName(), 0, runner.getDefaultTitle()))
    }
  }

  fun getExportTar(task: Task): ByteArray {
    val out = ByteArrayOutputStream()
    val tar = TarUtil.PosixTarArchiveOutputStream(out)
    fileService.readCollectionTar(task.id).use { files ->
      TarUtil.copyEntries(TarArchiveInputStream(files), tar, filter = { !TarUtil.isRoot(it) && it.name != TarUtil.CODEFREAK_DEFINITION_NAME })
    }

    val definition = TaskDefinition(
        title = task.title,
        id = task.id.toString(),
        description = task.body,
        hidden = task.hiddenFiles,
        protected = task.protectedFiles,
        evaluation = task.evaluationStepDefinitions.map {
          EvaluationDefinition(
              step = it.runnerName,
              options = it.options,
              title = it.title,
              id = it.id.toString()
          )
        },
        updatedAt = task.updatedAt.toString()
    ).let { yamlMapper.writeValueAsBytes(it) }

    tar.putArchiveEntry(TarArchiveEntry("codefreak.yml").also { it.size = definition.size.toLong() })
    tar.write(definition)
    tar.closeArchiveEntry()
    tar.close()
    return out.toByteArray()
  }

  fun getExportTar(tasks: Collection<Task>): ByteArray {
    val out = ByteArrayOutputStream()
    val tar = TarUtil.PosixTarArchiveOutputStream(out)

    tasks.forEach { task ->
      val taskTar = getExportTar(taskService.findTask(task.id)) // use findTask() on each task so everything is lazy initialized correctly
      tar.putArchiveEntry(TarArchiveEntry("${task.title}-${task.id}.tar").also { it.size = taskTar.size.toLong() })
      tar.write(taskTar)
      tar.closeArchiveEntry()
    }

    tar.close()
    return out.toByteArray()
  }
}
