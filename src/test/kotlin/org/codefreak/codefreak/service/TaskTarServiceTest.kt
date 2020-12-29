package org.codefreak.codefreak.service

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.nhaarman.mockitokotlin2.any
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class TaskTarServiceTest {
  private lateinit var taskTarService: TaskTarService

  @Mock
  private lateinit var mockTaskService: TaskService

  @Mock
  private lateinit var mockEvaluationService: EvaluationService

  @Mock
  private lateinit var mockEvaluationStepDefinitionRepository: EvaluationStepDefinitionRepository

  @Mock
  private lateinit var mockFileService: FileService

  @Before
  fun setUp() {
    // Note that the use of Mockito here encapsulates the tests of the
    // implementation details of the dependencies at the cost of performance,
    // because Mockito seems to trigger the garbage collector
    MockitoAnnotations.openMocks(this)

    taskTarService = TaskTarService()

    taskTarService.yamlMapper = YAMLMapper()

    `when`(mockTaskService.saveTask(any())).thenAnswer { it.arguments[0] }
    taskTarService.taskService = mockTaskService

    `when`(mockEvaluationService.getEvaluationRunner(any())).thenAnswer {
      object : EvaluationRunner {
        override fun getName(): String { return it.arguments[0] as String }
        override fun run(answer: Answer, options: Map<String, Any>) = listOf<Feedback>()
      }
    }
    taskTarService.evaluationService = mockEvaluationService

    `when`(mockEvaluationStepDefinitionRepository.save(any())).thenAnswer { it.arguments[0] }
    `when`(mockEvaluationStepDefinitionRepository.saveAll(any())).thenAnswer { it.arguments[0] }
    taskTarService.evaluationStepDefinitionRepository = mockEvaluationStepDefinitionRepository

    `when`(mockFileService.readCollectionTar(any())).thenReturn(ByteArrayInputStream(byteArrayOf()))
    `when`(mockFileService.writeCollectionTar(any())).thenReturn(ByteArrayOutputStream())
    taskTarService.fileService = mockFileService
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
