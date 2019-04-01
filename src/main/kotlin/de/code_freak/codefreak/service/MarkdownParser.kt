package de.code_freak.codefreak.service

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.parser.Parser
import org.springframework.stereotype.Component
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.util.options.MutableDataSet

@Component("markdown")
class MarkdownParser {
  private final val flexmarkOptions = MutableDataSet().apply {
    this.set(Parser.EXTENSIONS, listOf(TablesExtension.create(), StrikethroughExtension.create()))
    this.set(HtmlRenderer.SOFT_BREAK, "<br />\n")
  }
  private final val parser = Parser.builder(flexmarkOptions).build()!!
  private final val htmlRenderer = HtmlRenderer.builder(flexmarkOptions).build()!!

  /**
   * Parse input string as markdown and returns html
   */
  fun html(input: String) = htmlRenderer.render(parser.parse(input))
}
