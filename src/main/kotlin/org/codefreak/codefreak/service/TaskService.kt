package org.codefreak.codefreak.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID
import liquibase.util.StreamUtil
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.utils.IOUtils
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
    val existingTask = try {
      val definition = getYamlDefinition<TaskDefinition>(tarContent.inputStream())
      val taskId = UUID.fromString(definition.id ?: "")
      findTask(taskId).takeIf { it.owner == owner }
    } catch (e: IllegalArgumentException) {
      // Thrown if no valid id is found in the task definition
      null
    } catch (e: EntityNotFoundException) {
      // Thrown if task is not found in the repository
      null
    }

    existingTask?.let {
      return updateExistingTaskFromTar(it, tarContent, assignment, owner, position)
    }

    return createNewTaskFromTar(tarContent, assignment, owner, position)
  }

  private fun updateExistingTaskFromTar(task: Task, tarContent: ByteArray, assignment: Assignment?, owner: User, position: Long): Task {
    val definition = getYamlDefinition<TaskDefinition>(tarContent.inputStream())

    val updatedAt = Instant.parse(definition.updatedAt)
    val isUpToDate = !updatedAt.isAfter(task.updatedAt)

    if (isUpToDate) {
      return task
    }

    // TODO Update task

    return task
  }

  private fun createNewTaskFromTar(tarContent: ByteArray, assignment: Assignment?, owner: User, position: Long): Task {
    val definition = getYamlDefinition<TaskDefinition>(tarContent.inputStream())

    var task = Task(assignment, owner, position, definition.title, definition.description, 100)
    task.hiddenFiles = definition.hidden
    task.protectedFiles = definition.protected

    task = taskRepository.save(task)
    task.evaluationStepDefinitions = definition.evaluation
        .mapIndexed { index, it ->
          val runner = evaluationService.getEvaluationRunner(it.step)
          val title = it.title ?: runner.getDefaultTitle()
          val stepDefinition = EvaluationStepDefinition(task, runner.getName(), index, title, it.options)
          evaluationService.validateRunnerOptions(stepDefinition)
          stepDefinition
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
      TarUtil.copyEntries(tarContent.inputStream(), fileCollection, filter = { !it.name.equals("codefreak.yml", true) })
    }

    return task
  }

  @Transactional
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

  @Transactional
  fun getExportTar(taskId: UUID) = getExportTar(findTask(taskId))

  @Transactional
  fun getExportTar(task: Task): ByteArray {
    val out = ByteArrayOutputStream()
    val tar = TarUtil.PosixTarArchiveOutputStream(out)
    fileService.readCollectionTar(task.id).use { files ->
      TarUtil.copyEntries(TarArchiveInputStream(files), tar, filter = { !TarUtil.isRoot(it) && it.name != "codefreak.yml" })
    }

    val definition = TaskDefinition(
        title = task.title,
        id = task.id.toString(),
        description = task.body,
        hidden = task.hiddenFiles,
        protected = task.protectedFiles,
        evaluation = task.evaluationStepDefinitions.map { EvaluationDefinition(it.runnerName, it.options, it.title) },
        updatedAt = task.updatedAt.toString()
    ).let { ObjectMapper(YAMLFactory()).writeValueAsBytes(it) }

    tar.putArchiveEntry(TarArchiveEntry("codefreak.yml").also { it.size = definition.size.toLong() })
    tar.write(definition)
    tar.closeArchiveEntry()
    tar.close()
    return out.toByteArray()
  }

  @Transactional
  fun getExportTar(tasks: Collection<Task>): ByteArray {
    val out = ByteArrayOutputStream()
    val tar = TarUtil.PosixTarArchiveOutputStream(out)

    tasks.forEach { task ->
      val taskTar = getExportTar(findTask(task.id)) // use findTask() on each task so everything is lazy initialized correctly
      tar.putArchiveEntry(TarArchiveEntry("${task.title}-${task.id}.tar").also { it.size = taskTar.size.toLong() })
      tar.write(taskTar)
      tar.closeArchiveEntry()
    }

    tar.close()
    return out.toByteArray()
  }

  /**
   * Makes sure that evaluations can be run on this task even if answer files
   * have not changed. Call this every time you update evaluation settings.
   */
  @Transactional
  fun invalidateLatestEvaluations(task: Task) {
    task.evaluationSettingsChangedAt = Instant.now()
    taskRepository.save(task)
  }
}
