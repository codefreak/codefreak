package org.codefreak.codefreak.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import liquibase.util.StreamUtil
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.AssignmentRepository
import org.codefreak.codefreak.repository.EvaluationStepDefinitionRepository
import org.codefreak.codefreak.repository.TaskRepository
import org.codefreak.codefreak.service.evaluation.EvaluationService
import org.codefreak.codefreak.service.evaluation.isBuiltIn
import org.codefreak.codefreak.service.evaluation.runner.CommentRunner
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.PositionUtil
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.TarUtil.getYamlDefinition
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.util.UUID

@Service
class
TaskService : BaseService() {

  @Autowired
  private lateinit var taskRepository: TaskRepository

  @Autowired
  private lateinit var assignmentRepository: AssignmentRepository

  @Autowired
  private lateinit var evaluationService: EvaluationService

  @Autowired
  private lateinit var evaluationStepDefinitionRepository: EvaluationStepDefinitionRepository

  @Autowired
  private lateinit var fileService: FileService

  @Transactional
  fun findTask(id: UUID): Task = taskRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Task not found") }

  @Transactional
  fun createFromTar(tarContent: ByteArray, assignment: Assignment?, owner: User, position: Long): Task {
    val definition = getYamlDefinition<TaskDefinition>(tarContent.inputStream())
    var task = Task(assignment, owner, position, definition.title, definition.description, 100)
    task.hiddenFiles = definition.hidden
    task.protectedFiles = definition.protected

    task = taskRepository.save(task)
    task.evaluationStepDefinitions = definition.evaluation
        .mapIndexed { index, it ->
          val runner = evaluationService.getEvaluationRunner(it.step)
          val title = it.title ?: runner.getDefaultTitle()
          EvaluationStepDefinition(task, runner.getName(), index, title, it.options)
        }
        .toMutableSet()

    task.evaluationStepDefinitions
        .groupBy { it.runnerName }
        .forEach { (runnerName, definitions) ->
          if (definitions.size > 1 && evaluationService.getEvaluationRunner(runnerName).isBuiltIn()) {
            throw IllegalArgumentException("Evaluation step '$runnerName' can only be added once!")
          }
        }

    addBuiltInEvaluationSteps(task)

    evaluationStepDefinitionRepository.saveAll(task.evaluationStepDefinitions)
    task = taskRepository.save(task)
    fileService.writeCollectionTar(task.id).use { fileCollection ->
      TarUtil.copyEntries(tarContent.inputStream(), fileCollection) { !it.name.equals("codefreak.yml", true) }
    }
    return task
  }

  private fun addBuiltInEvaluationSteps(task: Task) {
    if (task.evaluationStepDefinitions.find { it.runnerName == CommentRunner.RUNNER_NAME } == null) {
      task.evaluationStepDefinitions.forEach { it.position++ }
      val runner = evaluationService.getEvaluationRunner(CommentRunner.RUNNER_NAME)
      task.evaluationStepDefinitions.add(EvaluationStepDefinition(task, runner.getName(), 0, runner.getDefaultTitle()))
    }
  }

  @Transactional
  fun createEmptyTask(owner: User): Task {
    return ByteArrayOutputStream().use {
      StreamUtil.copy(ClassPathResource("empty_task.tar").inputStream, it)
      createFromTar(it.toByteArray(), null, owner, 0)
    }
  }

  @Transactional
  fun deleteTask(task: Task) {
    task.assignment?.run {
      tasks.filter { it.position > task.position }.forEach { it.position-- }
      taskRepository.saveAll(tasks)
    }
    taskRepository.delete(task)
  }

  private fun applyDefaultRunners(taskDefinition: TaskDefinition): TaskDefinition {
    // add "comments" runner by default if not defined
    taskDefinition.run {
      if (evaluation.find { it.step == CommentRunner.RUNNER_NAME } == null) {
        return copy(evaluation = evaluation.toMutableList().apply {
          add(EvaluationDefinition(CommentRunner.RUNNER_NAME))
        })
      }
    }
    return taskDefinition
  }

  @Transactional
  fun saveTask(task: Task) = taskRepository.save(task)

  @Transactional
  fun setTaskPosition(task: Task, newPosition: Long) {
    val assignment = task.assignment
    require(assignment != null) { "Task is not part of an assignment" }

    PositionUtil.move(assignment.tasks, task.position, newPosition, { position }, { position = it })

    taskRepository.saveAll(assignment.tasks)
    assignmentRepository.save(assignment)
  }

  fun getTaskPool(userId: UUID) = taskRepository.findByOwnerIdAndAssignmentIsNullOrderByCreatedAt(userId)

  fun getExportTar(task: Task): ByteArray {
    val out = ByteArrayOutputStream()
    val tar = TarUtil.PosixTarArchiveOutputStream(out)
    fileService.readCollectionTar(task.id).use { files ->
      TarUtil.copyEntries(TarArchiveInputStream(files), tar) { it.name != "codefreak.yml" }
    }

    val definition = TaskDefinition(
        task.title,
        task.body,
        task.hiddenFiles,
        task.protectedFiles,
        task.evaluationStepDefinitions.map { EvaluationDefinition(it.runnerName, it.options, it.title) })
        .let { ObjectMapper(YAMLFactory()).writeValueAsBytes(it) }

    tar.putArchiveEntry(TarArchiveEntry("codefreak.yml").also { it.size = definition.size.toLong() })
    tar.write(definition)
    tar.closeArchiveEntry()
    tar.close()
    return out.toByteArray()
  }
}
