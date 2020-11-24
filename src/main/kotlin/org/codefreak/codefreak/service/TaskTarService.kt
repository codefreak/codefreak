package org.codefreak.codefreak.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import liquibase.util.StreamUtil
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
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskTarService : BaseService() {

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

  /**
   * Creates and saves a new task from the given tar.
   *
   * @param tarContent the tar containing a task
   * @param owner the owner of the task
   * @param assignment the assignment the task belongs to, if any
   * @param position the position of the task if it belongs to an assignment
   * @return the created task
   */
  @Transactional
  fun createFromTar(tarContent: ByteArray, owner: User, assignment: Assignment? = null, position: Long = 0L): Task {
    val definition = yamlMapper.getCodefreakDefinition<TaskDefinition>(tarContent.inputStream())

    var task = createTaskFromDefinition(definition, owner, assignment, position)

    task = taskService.saveTask(task)

    addEvaluationStepsFromDefinition(definition, task)
    validateEvaluationSteps(task)
    addBuiltInEvaluationSteps(task)

    evaluationStepDefinitionRepository.saveAll(task.evaluationStepDefinitions)

    task = taskService.saveTask(task)

    copyTaskFilesFromTar(task.id, tarContent)

    return task
  }

  /**
   * Creates a new task from a given TaskDefinition without the evaluation steps and the file contents.
   */
  private fun createTaskFromDefinition(
    definition: TaskDefinition,
    owner: User,
    assignment: Assignment? = null,
    position: Long = 0L
  ): Task {
    val task = Task(assignment, owner, position, definition.title, definition.description, 100)

    task.hiddenFiles = definition.hidden
    task.protectedFiles = definition.protected
    task.ideEnabled = definition.ide?.enabled ?: true
    task.ideImage = definition.ide?.image

    return task
  }

  private fun addEvaluationStepsFromDefinition(definition: TaskDefinition, task: Task) {
    task.evaluationStepDefinitions = getEvaluationStepDefinitions(definition, task)
  }

  /**
   * Returns a set of evaluation step definitions for a task defined in a given TaskDefinition.
   */
  private fun getEvaluationStepDefinitions(taskDefinition: TaskDefinition, task: Task) = taskDefinition.evaluation
      .mapIndexed { index, it ->
        val runner = evaluationService.getEvaluationRunner(it.step)
        val title = it.title ?: runner.getDefaultTitle()
        val stepDefinition = EvaluationStepDefinition(task, runner.getName(), index, title, it.options)
        evaluationService.validateRunnerOptions(stepDefinition)
        stepDefinition
      }
      .toMutableSet()

  private fun validateEvaluationSteps(task: Task) = task.evaluationStepDefinitions
      .groupBy { it.runnerName }
      .forEach { (runnerName, definitions) ->
        if (definitions.size > 1 && evaluationService.getEvaluationRunner(runnerName).isBuiltIn()) {
          throw IllegalArgumentException("Evaluation step '$runnerName' can only be added once!")
        }
      }

  private fun addBuiltInEvaluationSteps(task: Task) {
    if (task.evaluationStepDefinitions.find { it.runnerName == CommentRunner.RUNNER_NAME } == null) {
      task.evaluationStepDefinitions.forEach { it.position++ }
      val runner = evaluationService.getEvaluationRunner(CommentRunner.RUNNER_NAME)
      task.evaluationStepDefinitions.add(EvaluationStepDefinition(task, runner.getName(), 0, runner.getDefaultTitle()))
    }
  }

  private fun copyTaskFilesFromTar(taskId: UUID, tarContent: ByteArray) {
    fileService.writeCollectionTar(taskId).use { fileCollection ->
      TarUtil.copyEntries(tarContent.inputStream(), fileCollection, filter = { !it.name.equals(TarUtil.CODEFREAK_DEFINITION_NAME, true) })
    }
  }

  /**
   * Creates and saves multiple tasks from the given tar.
   * The tar has to contain the individual tasks as tar archives themselves.
   *
   * @param tarContent the tar containing multiple tasks as tar archives
   * @param owner the owner of the tasks
   * @param assignment the assignment the tasks belong to, if any
   * @return the created tasks.
   */
  fun createMultipleFromTar(tarContent: ByteArray, owner: User, assignment: Assignment? = null): List<Task> {
    val input = TarArchiveInputStream(ByteArrayInputStream(tarContent))
    val tasks = mutableListOf<Task>()
    generateSequence { input.nextTarEntry }
        .filter { it.isFile }
        .filter { it.name.endsWith(".tar", ignoreCase = true).or(it.name.endsWith(".zip", ignoreCase = true)) }
        .forEach {
          var content = IOUtils.toByteArray(input)
          if (!it.name.endsWith(".tar")) {
            val inputStream = ByteArrayInputStream(content)
            val outputStream = ByteArrayOutputStream()
            TarUtil.archiveToTar(inputStream, outputStream)
            content = outputStream.toByteArray()
          }
          tasks.add(createFromTar(content, owner, assignment))
        }
    return tasks
  }

  /**
   * Creates an empty task for the given User.
   *
   * @param owner the user who will own the task
   * @return the created task
   */
  @Transactional
  fun createEmptyTask(owner: User): Task {
    return ByteArrayOutputStream().use {
      StreamUtil.copy(ClassPathResource("empty_task.tar").inputStream, it)
      createFromTar(it.toByteArray(), owner)
    }
  }

  /**
   * Creates a tar archive of the task with the given id.
   *
   * @param taskId the id of the task to be exported
   * @return a tar archive containing the task
   */
  @Transactional
  fun getExportTar(taskId: UUID) = getExportTar(taskService.findTask(taskId))

  /**
   * Creates a tar archive of the given task.
   *
   * @param task the task to be exported
   * @return a tar archive containing the task
   */
  fun getExportTar(task: Task): ByteArray {
    val out = ByteArrayOutputStream()
    val tar = TarUtil.PosixTarArchiveOutputStream(out)
    fileService.readCollectionTar(task.id).use { files ->
      TarUtil.copyEntries(TarArchiveInputStream(files), tar, filter = { !TarUtil.isRoot(it) && it.name != TarUtil.CODEFREAK_DEFINITION_NAME })
    }

    val definition = TaskDefinition(
        task.title,
        task.body,
        task.hiddenFiles,
        task.protectedFiles,
        task.evaluationStepDefinitions.map {
          EvaluationDefinition(
              it.runnerName,
              it.options,
              it.title
          )
        }
    ).let { yamlMapper.writeValueAsBytes(it) }

    tar.putArchiveEntry(TarArchiveEntry("codefreak.yml").also { it.size = definition.size.toLong() })
    tar.write(definition)
    tar.closeArchiveEntry()
    tar.close()
    return out.toByteArray()
  }

  /**
   * Creates a tar archive containing the given tasks each as a tar archive.
   *
   * @param tasks the tasks to be exported
   * @return a tar containing the exported tasks
   */
  @Transactional(readOnly = true)
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
