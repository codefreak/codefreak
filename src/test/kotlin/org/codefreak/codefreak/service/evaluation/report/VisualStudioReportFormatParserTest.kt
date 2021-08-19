package org.codefreak.codefreak.service.evaluation.report

import org.codefreak.codefreak.entity.Feedback
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test

class VisualStudioReportFormatParserTest {

  private val parser = VisualStudioReportFormatParser()

    @Test
    fun `parse valid strings`() {
      val feedback = parser.parse("""
        # Taken from MS examples
        C:\sourcefile.cpp(134) : error C2143: syntax error : missing ';' before '}'
        LINK : fatal error LNK1104: cannot open file 'somelib.lib'

        # Taken from cpplint
        src/Calculator.h(1): error cpplint: [build/header_guard] #ifndef header guard has wrong style, please use: CALCULATOR_H_ [5]
        Done processing src/Calculator.h
        Total errors found: 2

        # Taken from ESLint
        /var/lib/jenkins/workspace/Releases/eslint Release/eslint/fullOfProblems.js(1,10): warning no-unused-vars : 'addOne' is defined but never used.

        # Taken from dotnet-format
        src/AddFunction/AddFunction.cs(8,10): error WHITESPACE: Fix whitespace formatting. Replace 2 characters with '\n\s\s\s\s\s\s\s\s\s\s\s\s'. [src/AddFunction/AddFunction.csproj]
      """.trimIndent())
      MatcherAssert.assertThat(feedback, Matchers.hasSize(5))
      MatcherAssert.assertThat(
          feedback,
          Matchers.containsInAnyOrder(
              Matchers.allOf(
                  Matchers.hasProperty(
                      "summary",
                      Matchers.equalTo("syntax error : missing ';' before '}'")
                  ),
                  Matchers.hasProperty("status", Matchers.equalTo(Feedback.Status.FAILED)),
                  Matchers.hasProperty("severity", Matchers.equalTo(Feedback.Severity.MAJOR)),
                  Matchers.hasProperty("group", Matchers.equalTo("C2143")),
                  Matchers.hasProperty("fileContext", Matchers.allOf<Feedback.FileContext>(
                      Matchers.hasProperty("path", Matchers.equalTo("C:\\sourcefile.cpp")),
                      Matchers.hasProperty("lineStart", Matchers.equalTo(134))
                  ))
              ),
              Matchers.allOf(
                  Matchers.hasProperty(
                      "summary",
                      Matchers.equalTo("cannot open file 'somelib.lib'")
                  ),
                  Matchers.hasProperty("status", Matchers.equalTo(Feedback.Status.FAILED)),
                  Matchers.hasProperty("severity", Matchers.equalTo(Feedback.Severity.MAJOR)),
                  Matchers.hasProperty("group", Matchers.equalTo("LNK1104")),
                  Matchers.hasProperty("fileContext", Matchers.nullValue())
              ),
              Matchers.allOf(
                  Matchers.hasProperty(
                      "summary",
                      Matchers.equalTo("[build/header_guard] #ifndef header guard has wrong style, please use: CALCULATOR_H_")
                  ),
                  Matchers.hasProperty("status", Matchers.equalTo(Feedback.Status.FAILED)),
                  Matchers.hasProperty("severity", Matchers.equalTo(Feedback.Severity.MAJOR)),
                  Matchers.hasProperty("group", Matchers.equalTo("cpplint")),
                  Matchers.hasProperty("fileContext", Matchers.allOf<Feedback.FileContext>(
                      Matchers.hasProperty("path", Matchers.equalTo("src/Calculator.h")),
                      Matchers.hasProperty("lineStart", Matchers.equalTo(1))
                  ))
              ),
              Matchers.allOf(
                  Matchers.hasProperty(
                      "summary",
                      Matchers.equalTo("'addOne' is defined but never used.")
                  ),
                  Matchers.hasProperty("status", Matchers.equalTo(Feedback.Status.FAILED)),
                  Matchers.hasProperty("severity", Matchers.equalTo(Feedback.Severity.MINOR)),
                  Matchers.hasProperty("group", Matchers.equalTo("no-unused-vars")),
                  Matchers.hasProperty("fileContext", Matchers.allOf<Feedback.FileContext>(
                      Matchers.hasProperty("path", Matchers.equalTo("/var/lib/jenkins/workspace/Releases/eslint Release/eslint/fullOfProblems.js")),
                      Matchers.hasProperty("lineStart", Matchers.equalTo(1)),
                      Matchers.hasProperty("columnStart", Matchers.equalTo(10))
                  ))
              ),
              Matchers.allOf(
                  Matchers.hasProperty(
                      "summary",
                      Matchers.equalTo("Fix whitespace formatting. Replace 2 characters with '\\n\\s\\s\\s\\s\\s\\s\\s\\s\\s\\s\\s\\s'.")
                  ),
                  Matchers.hasProperty("status", Matchers.equalTo(Feedback.Status.FAILED)),
                  Matchers.hasProperty("severity", Matchers.equalTo(Feedback.Severity.MAJOR)),
                  Matchers.hasProperty("group", Matchers.equalTo("WHITESPACE")),
                  Matchers.hasProperty("fileContext", Matchers.allOf<Feedback.FileContext>(
                      Matchers.hasProperty("path", Matchers.equalTo("src/AddFunction/AddFunction.cs")),
                      Matchers.hasProperty("lineStart", Matchers.equalTo(8)),
                      Matchers.hasProperty("columnStart", Matchers.equalTo(10))
                  ))
              )
          )
      )
    }
}
