package org.codefreak.codefreak.service.evaluation

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.codefreak.codefreak.EXTERNAL_INTEGRATION_TEST
import org.codefreak.codefreak.service.WorkspaceBaseTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.aMapWithSize
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.equalToCompressingWhiteSpace
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.Matchers.hasProperty
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.util.StreamUtils

@TestPropertySource(properties = ["codefreak.evaluation.backend=workspace"])
@Import(WorkspaceEvaluationBackend::class)
@Tag(EXTERNAL_INTEGRATION_TEST)
internal class WorkspaceEvaluationBackendTest : WorkspaceBaseTest() {
  @Autowired
  private lateinit var evaluationBackend: WorkspaceEvaluationBackend

  private val evaluationStepId = UUID(0, 0)

  @MockBean
  private lateinit var evaluationStepService: EvaluationStepService

  @BeforeAll
  fun beforeAll() {
    val tar = createTarWithEntries(mapOf("file.txt" to "foo"))
    whenever(evaluationStepService.getFilesForEvaluation(evaluationStepId)).thenReturn(tar)
  }

  @Test
  fun runEvaluation() {
    val runConfig = object : EvaluationRunConfig {
      override val id = evaluationStepId
      override val script = """
        #!/bin/bash
        echo "Hello ${'$'}FOO"
        echo "filecontent" > file.txt
        exit 123
      """.trimIndent()
      override val environment: Map<String, String> = mapOf("FOO" to "World")
      override val imageName: String = "" // currently not used
      override val workingDirectory: String = "" // not used by workspace backend
    }
    val capturedFiles = mutableMapOf<String, String>()
    val resultProcessor = mock<EvaluationResultProcessor<String>> {
      on { invoke(any()) } doAnswer {
        val arg = it.getArgument<EvaluationResult>(0)
        arg.consumeFiles("*") { fileName, fileContent ->
          capturedFiles[fileName] = StreamUtils.copyToString(fileContent, StandardCharsets.UTF_8)
        }
        arg.output
      }
    }
    evaluationBackend.runEvaluation(runConfig, resultProcessor)
    val captor = argumentCaptor<EvaluationResult>()
    verify(resultProcessor).invoke(captor.capture())
    val evaluationResult = captor.firstValue
    assertThat(
      evaluationResult, allOf(
        hasProperty("exitCode", equalTo(123)),
        hasProperty("output", equalToCompressingWhiteSpace("Hello World"))
      )
    )
    assertThat(
      capturedFiles,
      allOf(
        aMapWithSize(1),
        hasEntry("file.txt", "filecontent\n")
      )
    )
  }
}
