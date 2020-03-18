package de.code_freak.codefreak.service.evaluation.runner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Feedback
import de.code_freak.codefreak.entity.Feedback.FileContext
import de.code_freak.codefreak.entity.Feedback.Severity
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.evaluation.EvaluationRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CodeclimateRunner : EvaluationRunner {
  companion object {
    /**
     * Map from CodeClimate severities to Feedback severity
     * https://github.com/codeclimate/spec/blob/master/SPEC.md#data-types
     */
    private val CODECLIMATE_SEVERITY_MAP: Map<String, Severity> = mapOf(
        "info" to Severity.INFO,
        "minor" to Severity.MINOR,
        "major" to Severity.MAJOR,
        "critical" to Severity.CRITICAL,
        "blocker" to Severity.CRITICAL
    )
  }

  @Autowired
  private lateinit var containerService: ContainerService

  override fun getName(): String {
    return "codeclimate"
  }

  override fun run(answer: Answer, options: Map<String, Any>): List<Feedback> {
    val codeclimateJson = containerService.runCodeclimate(answer)
    return this.parseCodeclimateJson(codeclimateJson).map { issue ->
      Feedback(issue.description).apply {
        group = "${issue.engine_name}/${issue.check_name}"
        longDescription = issue.content?.body
        status = Feedback.Status.FAILED
        severity = CODECLIMATE_SEVERITY_MAP[issue.severity]
        fileContext = FileContext(
            issue.location.path,
            lineStart = issue.location.lines.begin,
            lineEnd = issue.location.lines.end
        )
      }
    }
  }

  private fun parseCodeclimateJson(content: String): List<Issue> {
    var json = content
    // codeclimate may print a message before the actual JSON
    if (!json.startsWith("[")) {
      json = json.drop(content.indexOfFirst { it == '\n' } + 1)
    }
    val mapper = ObjectMapper()
    val results = mapper.readValue(json, Array<Result>::class.java)
    return results.filterIsInstance<Issue>()
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes(
      Type(value = Issue::class, name = "issue"),
      Type(value = Measurement::class, name = "measurement")
  )
  @JsonIgnoreProperties(ignoreUnknown = true)
  open class Result {
    var engine_name = ""
  }

  class Measurement : Result() {
    var value = 0
    var name = ""
  }

  class Issue : Result() {
    var description = ""
    var check_name = ""
    var content: Content? = null
    var categories = listOf<String>()
    var location: Location = Location()
    var severity: String? = null
    var fingerprint: String? = null

    class Content {
      var body = ""
    }

    // TODO support other location formats (needs custom deserializer)
    //      see https://github.com/codeclimate/spec/blob/master/SPEC.md#locations
    class Location {
      var path = ""
      var lines = Lines()

      class Lines {
        var begin = 0
        var end = 0
      }
    }
  }
}
