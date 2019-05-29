package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.io.File
import java.util.Date

@Service
class LatexService {
  class SourceCodeView(
    val filename: String,
    val lastModified: Date,
    val content: String,
    val syntax: String? = null
  ) {
    val escapedFilename: String get() = escapeString(filename)

    companion object {
      fun fromFile(file: File, baseDir: File): SourceCodeView {
        return SourceCodeView(
            filename = file.relativeTo(baseDir).path,
            lastModified = Date(file.lastModified()),
            content = file.readText()
        )
      }
    }
  }

  companion object {
    val LATEX_SPECIAL_CHARS = listOf("_", "^", "~", "$", "%", "#", "&", "{", "}")
    fun escapeString(input: String): String {
      return LATEX_SPECIAL_CHARS.fold(input) { text, char ->
        return text.replace(char, "\\$char")
      }
    }
  }

  @Autowired
  @Qualifier("latexTemplate")
  lateinit var latexTemplateEngine: TemplateEngine

  @Autowired
  lateinit var containerService: ContainerService

  fun answerToPdf(answer: Answer): ByteArray {
    val tmpDir = createTempDir()
    TarUtil.extractTarToDirectory(answer.files!!, tmpDir)
    val files = tmpDir.walkTopDown().filter { file -> file.extension == "java" }.toList()
    val ctx = Context()
    ctx.setVariable("files", files.map({ file -> SourceCodeView.fromFile(file, tmpDir) }))
    val tex = latexTemplateEngine.process("source-code-listings", ctx)
    val pdf = getPdf(tex)
    tmpDir.deleteRecursively()
    return pdf
  }

  /**
   * TODO: use Docker container so you do not have to install latex
   */
  fun getPdf(texSource: String): ByteArray {
    val tmpDir = createTempDir()
    tmpDir.mkdirs()
    val texFile = File(tmpDir, "document.tex")
    texFile.writeText(texSource)
    val inputArchive = TarUtil.createTarFromDirectory(tmpDir)
    val outputArchive = containerService.latexConvert(inputArchive, texFile.name)
    TarUtil.extractTarToDirectory(outputArchive, tmpDir)
    val pdfContent = File(tmpDir, "document.pdf").readBytes()
    tmpDir.deleteRecursively()
    return pdfContent
  }
}
