package org.codefreak.codefreak.frontend

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.charset.Charset
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.web.servlet.View

/**
 * Error view that injects a script into a HTML template before the first <script tag.
 * It exposes a global JS variable named "__CODEFREAK_ERROR" that includes all error attributes
 */
@Component("error")
class InjectedScriptErrorView : View {
  @Autowired
  private lateinit var applicationContext: ApplicationContext

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  private fun buildErrorJsonScriptTag(data: Map<String, *>?): String {
    val json = objectMapper.writeValueAsString(data)
    return "<script>const __CODEFREAK_ERROR = $json;</script>"
  }

  override fun render(model: MutableMap<String, *>?, request: HttpServletRequest, response: HttpServletResponse) {
    val error = applicationContext.getResource("classpath:static/index.html")
    val content = IOUtils.toString(error.inputStream, Charset.defaultCharset())
    var errorScriptTag = buildErrorJsonScriptTag(model)
    if (content.contains("\n")) {
      errorScriptTag += "\n"
    }
    val body = content.replaceFirst("<script", "$errorScriptTag<script")
    response.writer.append(body)
  }
}
