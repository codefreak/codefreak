package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Submission
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

  fun submissionToPdf(submission: Submission): ByteArray {
    // map of answer-uuid to SCV of answers files
    val files = submission.answers.map {
      it.id to getSourceCodeFilesFromTar(it.files!!, setOf("java"))
    }.toMap()
    val ctx = Context()
    ctx.setVariable("files", files)
    ctx.setVariable("submission", submission)
    val tex = latexTemplateEngine.process("submission", ctx)
    val pdf = getPdf(tex)
    return pdf
  }

  fun answerToPdf(answer: Answer): ByteArray {
    val answerFiles = answer.files ?: throw IllegalArgumentException("Answer has no files")
    val ctx = Context()
    ctx.setVariable("files", getSourceCodeFilesFromTar(answerFiles, setOf("java")))
    val tex = latexTemplateEngine.process("source-code-listings", ctx)
    val pdf = getPdf(tex)
    return pdf
  }

  fun getSourceCodeFilesFromTar(tar: ByteArray, extensions: Set<String>): List<SourceCodeView> {
    val tmpDir = createTempDir()
    TarUtil.extractTarToDirectory(tar, tmpDir)
    val files = tmpDir.walkTopDown().filter { file -> extensions.contains(file.extension) }
        .toList()
        .map { file -> SourceCodeView.fromFile(file, tmpDir) }
    tmpDir.deleteRecursively()
    return files
  }

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
