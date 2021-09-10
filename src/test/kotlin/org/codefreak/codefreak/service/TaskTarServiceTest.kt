package org.codefreak.codefreak.service

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nhaarman.mockitokotlin2.any
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.random.Random
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.EvaluationStepDefinitionRepository
import org.codefreak.codefreak.service.file.FileService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.Spy

class TaskTarServiceTest {
  private val user = User("Dummy user")
  private val task = Task(null, user, 0, "Dummy task")

  @InjectMocks
  private lateinit var taskTarService: TaskTarService

  @Mock
  private lateinit var mockTaskService: TaskService

  @Mock
  private lateinit var mockEvaluationStepDefinitionRepository: EvaluationStepDefinitionRepository

  @Mock
  private lateinit var mockFileService: FileService

  @Spy
  internal lateinit var yamlMapper: YAMLMapper

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    yamlMapper.registerKotlinModule()
    `when`(mockTaskService.saveTask(any())).thenAnswer { it.arguments[0] }

    `when`(mockEvaluationStepDefinitionRepository.save(any())).thenAnswer { it.arguments[0] }
    // circumvent impossible type inference with "Nothing" in Kotlin 1.4.30
    // https://youtrack.jetbrains.com/issue/KT-44045
    `when`(mockEvaluationStepDefinitionRepository.saveAll<Nothing>(any())).thenAnswer { it.arguments[0] }

    `when`(mockFileService.readCollectionTar(any())).thenReturn(ByteArrayInputStream(byteArrayOf()))
    `when`(mockFileService.writeCollectionTar(any())).thenReturn(ByteArrayOutputStream())
  }

  @Test
  fun `EvaluationStepDefinitions keep positions on import`() {
    task.evaluationStepDefinitions = createUnorderedEvaluationStepDefinitions(task)

    val importedTask = exportAndReimportTask(task, user)

    task.evaluationStepDefinitions.values.forEach {
      val importedDefinition = importedTask.evaluationStepDefinitions[it.key]
          ?: fail("EvaluationStep does not exist after re-import")
      Assertions.assertEquals(
          it.position,
          importedDefinition.position,
        "Step ${it.title} has an incorrect position"
      )
    }
  }

  @Test
  fun `EvaluationStepDefinitions keep (in-)active status`() {
    task.evaluationStepDefinitions = createEvaluationStepDefinitionsWithInactiveSteps(task)

    val importedTask = exportAndReimportTask(task, user)

    task.evaluationStepDefinitions.values.forEach {
      val importedDefinition = importedTask.evaluationStepDefinitions[it.key]
          ?: fail("EvaluationStep does not exist after re-import")
      Assertions.assertEquals(
          it.active,
          importedDefinition.active,
        "Step ${it.title} has an incorrect active state"
      )
    }
  }

  private fun createEvaluationStepDefinitionsWithInactiveSteps(task: Task): MutableMap<String, EvaluationStepDefinition> {
    val definitions = createUnorderedEvaluationStepDefinitions(task)

    definitions.values.first().active = false
    definitions.values.last().active = false

    return definitions
  }

  private fun createStepDefinition(task: Task, position: Int): EvaluationStepDefinition {
    return EvaluationStepDefinition(
        "step-${position + 1}",
        task,
        position,
        "Step ${position + 1}",
        "echo 'Step $position'",
        EvaluationStepDefinition.EvaluationStepReportDefinition(
            "junit-xml",
            "report.xml"
        )
    )
  }

  /**
   * Create a random list of integers
   */
  private fun createUnorderedEvaluationStepDefinitions(task: Task) = (0..8).shuffled(Random(1337)).map { position ->
    createStepDefinition(
        task,
        position
    )
  }.associateBy { it.key }.toMutableMap()

  private fun exportAndReimportTask(task: Task, user: User): Task {
    val exportedTar = taskTarService.getExportTar(task)
    return taskTarService.createFromTar(exportedTar, user)
  }
}
