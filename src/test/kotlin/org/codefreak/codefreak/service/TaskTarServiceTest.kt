package org.codefreak.codefreak.service

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Optional
import java.util.UUID
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.EvaluationStepDefinitionRepository
import org.codefreak.codefreak.service.evaluation.EvaluationRunner
import org.codefreak.codefreak.service.evaluation.EvaluationService
import org.codefreak.codefreak.service.file.FileService
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TaskTarServiceTest {
  private lateinit var taskTarService: TaskTarService

  @Before
  fun setUp() {
    taskTarService = TaskTarService()
    taskTarService.yamlMapper = YAMLMapper()
    taskTarService.taskService = createMockTaskService()
    taskTarService.evaluationService = createMockEvaluationService()
    taskTarService.evaluationStepDefinitionRepository = createMockEvaluationStepDefinitionRepository()
    taskTarService.fileService = createMockFileService()
  }

  // Create own mock object instead of using mockito, because it is quite slow, might be the garbage collector
  private fun createMockFileService() = object : FileService {
    override fun readCollectionTar(collectionId: UUID) = ByteArrayInputStream(byteArrayOf())
    override fun writeCollectionTar(collectionId: UUID) = ByteArrayOutputStream()
    override fun collectionExists(collectionId: UUID) = true
    override fun deleteCollection(collectionId: UUID) {}
    override fun listFiles(collectionId: UUID) = listOf<String>()
    override fun createFile(collectionId: UUID, path: String) {}
    override fun createDirectory(collectionId: UUID, path: String) {}
    override fun containsFile(collectionId: UUID, path: String) = true
    override fun containsDirectory(collectionId: UUID, path: String) = true
    override fun deleteFile(collectionId: UUID, path: String) {}
    override fun filePutContents(collectionId: UUID, path: String) = ByteArrayOutputStream()
    override fun getFileContents(collectionId: UUID, path: String) = ByteArrayInputStream(byteArrayOf())
    override fun moveFile(collectionId: UUID, from: String, to: String) {}
  }

  // Create own mock object instead of using mockito, because it is quite slow, might be the garbage collector
  private fun createMockTaskService() = object : TaskService() {
    override fun saveTask(task: Task) = task
  }

  // Create own mock object instead of using mockito, because it is quite slow, might be the garbage collector
  private fun createMockEvaluationService() = object : EvaluationService() {
    override fun getEvaluationRunner(name: String) = object : EvaluationRunner {
      override fun getName() = name
      override fun run(answer: Answer, options: Map<String, Any>) = listOf<Feedback>()
    }
  }

  // Create own mock object instead of using mockito, because it is quite slow, might be the garbage collector
  private fun createMockEvaluationStepDefinitionRepository() = object : EvaluationStepDefinitionRepository {
    override fun <S : EvaluationStepDefinition?> save(entity: S) = entity
    override fun <S : EvaluationStepDefinition?> saveAll(entities: MutableIterable<S>) = entities
    override fun findById(id: UUID): Optional<EvaluationStepDefinition> = Optional.empty()
    override fun existsById(id: UUID) = true
    override fun findAll() = mutableListOf<EvaluationStepDefinition>()
    override fun findAllById(ids: MutableIterable<UUID>) = mutableListOf<EvaluationStepDefinition>()
    override fun count() = 0L
    override fun deleteById(id: UUID) {}
    override fun delete(entity: EvaluationStepDefinition) {}
    override fun deleteAll(entities: MutableIterable<EvaluationStepDefinition>) {}
    override fun deleteAll() {}
  }

  @Test
  fun `EvaluationStepDefinitions keep positions on import`() {
    val user = User("Dummy user")
    val task = Task(null, user, 0, "Dummy task")
    task.evaluationStepDefinitions = createUnorderedEvaluationStepDefinitionSet(task)

    val exportedTar = taskTarService.getExportTar(task)
    val importedTask = taskTarService.createFromTar(exportedTar, user)

    task.evaluationStepDefinitions.forEach {
      val importedDefinition = findDefinition(it.title, importedTask.evaluationStepDefinitions)
      assertEquals(
        "Step ${it.title} has an incorrect position",
        it.position,
        importedDefinition.position
      )
    }
  }

  private fun createUnorderedEvaluationStepDefinitionSet(task: Task) = mutableSetOf(
    EvaluationStepDefinition(task, "comments", 0, "Comments"),
    EvaluationStepDefinition(task, "codeclimate", 1, "Code Quality 1"),
    EvaluationStepDefinition(task, "junit", 5, "Unit tests 1"),
    EvaluationStepDefinition(task, "codeclimate", 2, "Code Quality 2"),
    EvaluationStepDefinition(task, "junit", 6, "Unit tests 2"),
    EvaluationStepDefinition(task, "codeclimate", 3, "Code Quality 3"),
    EvaluationStepDefinition(task, "junit", 7, "Unit tests 3"),
    EvaluationStepDefinition(task, "junit", 8, "Unit tests 4"),
    EvaluationStepDefinition(task, "codeclimate", 4, "Code Quality 4")
  )

  private fun findDefinition(title: String, definitions: Set<EvaluationStepDefinition>): EvaluationStepDefinition =
    definitions.find { it.title == title } ?: throw IllegalStateException("Definition $title does not exist")
}
