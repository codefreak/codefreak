package org.codefreak.codefreak.service.evaluation.report

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.codefreak.codefreak.entity.Feedback
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test

class CheckstyleReportFormatParserTest {
  private val parser = CheckstyleReportFormatParser(
      XmlMapper()
          .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .registerKotlinModule()
  )

  @Test
  fun parseStandard() {
    val feedback = parser.parse(
        """
<?xml version="1.0" encoding="UTF-8"?>
<checkstyle version="8.45.1">
    <file name="/home/coder/project/src/main/java/Calculator.java">
        <error line="2" severity="warning" message="First sentence of Javadoc is missing an ending period."
               source="com.puppycrawl.tools.checkstyle.checks.javadoc.SummaryJavadocCheck"/>
        <error line="4" severity="error" message="Line is longer than 100 characters (found 115)."
               source="com.puppycrawl.tools.checkstyle.checks.sizes.LineLengthCheck"/>
    </file>
    <file name="/home/coder/project/src/main/java/Main.java">
        <error line="13" column="5" severity="warning"
               message="&apos;method def rcurly&apos; has incorrect indentation level 4, expected level should be 2."
               source="com.puppycrawl.tools.checkstyle.checks.indentation.IndentationCheck"/>
    </file>
</checkstyle>
""".trimIndent()
    )
    MatcherAssert.assertThat(feedback, Matchers.hasSize(3))
    MatcherAssert.assertThat(
        feedback,
        Matchers.containsInAnyOrder(
            Matchers.allOf(
                Matchers.hasProperty(
                    "summary",
                    Matchers.equalTo("First sentence of Javadoc is missing an ending period.")
                ),
                Matchers.hasProperty("status", Matchers.equalTo(Feedback.Status.FAILED)),
                Matchers.hasProperty("severity", Matchers.equalTo(Feedback.Severity.MAJOR))
            ),
            Matchers.allOf(
                Matchers.hasProperty("summary", Matchers.equalTo("Line is longer than 100 characters (found 115).")),
                Matchers.hasProperty("status", Matchers.equalTo(Feedback.Status.FAILED)),
                Matchers.hasProperty("severity", Matchers.equalTo(Feedback.Severity.CRITICAL))
            ),
            Matchers.allOf(
                Matchers.hasProperty(
                    "summary",
                    Matchers.equalTo("'method def rcurly' has incorrect indentation level 4, expected level should be 2.")
                ),
                Matchers.hasProperty("status", Matchers.equalTo(Feedback.Status.FAILED)),
                Matchers.hasProperty("severity", Matchers.equalTo(Feedback.Severity.MAJOR))
            )
        )
    )
  }

  /**
   * @see <a href="https://github.com/checkstyle/checkstyle/blob/master/src/test/resources/com/puppycrawl/tools/checkstyle/xmllogger/ExpectedXMLLoggerException2.xml">ExpectedXMLLoggerException2.xml</a>
   */
  @Test
  fun `parse with exception in file`() {
    val feedback = parser.parse(
        """
<?xml version="1.0" encoding="UTF-8"?>
<checkstyle version="">
<file name="Test.java">
<exception>
stackTrace
example
</exception>
</file>
</checkstyle>
""".trimIndent()
    )
    MatcherAssert.assertThat(feedback, Matchers.hasSize(1))
    MatcherAssert.assertThat(
        feedback,
        Matchers.contains(
            Matchers.allOf(
                Matchers.hasProperty("summary", Matchers.equalTo("Exception in file Test.java")),
                Matchers.hasProperty("longDescription", Matchers.equalTo("stackTrace\nexample")),
                Matchers.hasProperty("status", Matchers.equalTo(Feedback.Status.FAILED)),
                Matchers.hasProperty("severity", Matchers.equalTo(Feedback.Severity.CRITICAL))
            )
        )
    )
  }

  /**
   * @see <a href="https://github.com/checkstyle/checkstyle/blob/master/src/test/resources/com/puppycrawl/tools/checkstyle/xmllogger/ExpectedXMLLoggerException.xml">ExpectedXMLLoggerException.xml</a>
   */
  @Test
  fun `parse with exception in root`() {
    val feedback = parser.parse(
        """
<?xml version="1.0" encoding="UTF-8"?>
<checkstyle version="">
<exception>
stackTrace
example
</exception>
</checkstyle>
""".trimIndent()
    )
    MatcherAssert.assertThat(feedback, Matchers.hasSize(1))
    MatcherAssert.assertThat(
        feedback,
        Matchers.contains(
            Matchers.allOf(
                Matchers.hasProperty("summary", Matchers.equalTo("Failed to compile")),
                Matchers.hasProperty("longDescription", Matchers.equalTo("stackTrace\nexample")),
                Matchers.hasProperty("status", Matchers.equalTo(Feedback.Status.FAILED)),
                Matchers.hasProperty("severity", Matchers.equalTo(Feedback.Severity.CRITICAL))
            )
        )
    )
  }

  @Test
  fun `parse empty works correctly`() {
    val feedback = parser.parse(
        """
      <?xml version="1.0" encoding="UTF-8"?>
      <checkstyle></checkstyle>
    """.trimIndent()
    )
    MatcherAssert.assertThat(feedback, Matchers.hasSize(0))
  }
}
