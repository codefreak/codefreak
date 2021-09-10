package org.codefreak.codefreak.service.evaluation.report

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.codefreak.codefreak.entity.Feedback
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test

class PylintJsonReportFormatParserTest {
  private val parser = PylintJsonReportFormatParser(
      JsonMapper().registerKotlinModule()
  )

    @Test
    fun parse() {
      val feedback = parser.parse("""
        [
            {
                "type": "convention",
                "module": "main",
                "obj": "",
                "line": 1,
                "column": 0,
                "path": "main.py",
                "symbol": "missing-module-docstring",
                "message": "Missing module docstring",
                "message-id": "C0114"
            },
            {
                "type": "convention",
                "module": "main",
                "obj": "add",
                "line": 1,
                "column": 0,
                "path": "main.py",
                "symbol": "invalid-name",
                "message": "Argument name \"a\" doesn't conform to snake_case naming style",
                "message-id": "C0103"
            },
            {
                "type": "warning",
                "module": "main",
                "obj": "add",
                "line": 1,
                "column": 8,
                "path": "main.py",
                "symbol": "unused-argument",
                "message": "Unused argument 'a'",
                "message-id": "W0613"
            }
        ]
      """.trimIndent())
      MatcherAssert.assertThat(feedback, Matchers.hasSize(3))
      MatcherAssert.assertThat(
          feedback,
          Matchers.containsInAnyOrder(
              Matchers.allOf(
                  Matchers.hasProperty(
                      "summary",
                      Matchers.equalTo("Missing module docstring")
                  ),
                  Matchers.hasProperty("status", Matchers.equalTo(Feedback.Status.FAILED)),
                  Matchers.hasProperty("severity", Matchers.equalTo(Feedback.Severity.MINOR))
              ),
              Matchers.allOf(
                  Matchers.hasProperty(
                      "summary",
                      Matchers.equalTo("Argument name \"a\" doesn't conform to snake_case naming style")
                  ),
                  Matchers.hasProperty("status", Matchers.equalTo(Feedback.Status.FAILED)),
                  Matchers.hasProperty("severity", Matchers.equalTo(Feedback.Severity.MINOR))
              ),
              Matchers.allOf(
                  Matchers.hasProperty(
                      "summary",
                      Matchers.equalTo("Unused argument 'a'")
                  ),
                  Matchers.hasProperty("status", Matchers.equalTo(Feedback.Status.FAILED)),
                  Matchers.hasProperty("severity", Matchers.equalTo(Feedback.Severity.MAJOR)),
                  Matchers.hasProperty("fileContext", Matchers.allOf<Feedback.FileContext>(
                      Matchers.hasProperty("path", Matchers.equalTo("main.py")),
                      Matchers.hasProperty("lineStart", Matchers.equalTo(1)),
                      Matchers.hasProperty("columnStart", Matchers.equalTo(8))
                  ))
              )
          )
      )
    }
}
