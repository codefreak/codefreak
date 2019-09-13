package de.code_freak.codefreak.service.evaluation.runner

import com.fasterxml.jackson.databind.ObjectMapper
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.util.TarUtil
import de.code_freak.codefreak.util.withTrailingSlash
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.openmbee.junit.JUnitMarshalling
import org.openmbee.junit.model.JUnitTestSuite
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Arrays

@Component
class JUnitRunner : CommandLineRunner() {

  private class Results(val executions: List<Execution>, val xmlReports: List<ByteArray>) {
    private constructor() : this(emptyList(), emptyList())
  }

  private class RenderResults(val executions: List<Execution>, val testSuites: List<JUnitTestSuite>) {
    private constructor() : this(emptyList(), emptyList())
  }

  private val mapper = ObjectMapper()

  override fun getName(): String {
    return "junit"
  }

  override fun run(answer: Answer, options: Map<String, Any>): String {
    val resultsPath = options.get("results-path", String::class) ?: "build/test-results/test"
    val resultsPattern = (TarUtil.normalizeEntryName(resultsPath).withTrailingSlash() + "TEST-.+\\.xml").toRegex()
    val defaultOptions = mapOf(
        "image" to "gradle",
        "project-path" to "/home/gradle/project",
        "stop-on-fail" to true,
        "commands" to listOf("gradle testClasses", "gradle test")
    )
    val xmlReports = mutableListOf<ByteArray>()
    val executions = super.executeCommands(answer, defaultOptions + options) { files ->
      val tar = TarArchiveInputStream(files)
      generateSequence { tar.nextTarEntry }.forEach {
        if (it.isFile && TarUtil.normalizeEntryName(it.name).matches(resultsPattern)) {
          val out = ByteArrayOutputStream()
          StreamUtils.copy(tar, out)
          xmlReports.add(out.toByteArray())
        }
      }
    }
    return mapper.writeValueAsString(Results(executions, xmlReports))
  }

  override fun parseResultContent(content: ByteArray): Any {
    val results = mapper.readValue(content, Results::class.java)
    val testSuites = results.xmlReports.map { JUnitMarshalling.unmarshalTestSuite(ByteArrayInputStream(it)) }
    return RenderResults(results.executions, testSuites)
  }
}
