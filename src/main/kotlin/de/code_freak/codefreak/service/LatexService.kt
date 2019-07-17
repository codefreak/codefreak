package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.TarUtil
import de.code_freak.codefreak.util.afterClose
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
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
    private val LATEX_SPECIAL_CHARS = listOf("_", "^", "~", "$", "%", "#", "&", "{", "}")
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

  @Autowired
  lateinit var fileService: FileService

  fun submissionToPdf(submission: Submission, out: OutputStream) {
    // map of answer-uuid to SCV of answers files
    val files = submission.answers.map { answer ->
      answer.id to fileService.readCollectionTar(answer.id).use { getSourceCodeFilesFromTar(it, setOf("java")) }
    }.toMap()
    val ctx = Context()
    ctx.setVariable("files", files)
    ctx.setVariable("submission", submission)
    val tex = latexTemplateEngine.process("submission", ctx)
    getPdf(tex, out)
  }

  fun answerToPdf(answer: Answer, out: OutputStream) {
    val ctx = Context()
    fileService.readCollectionTar(answer.id).use {
      ctx.setVariable("files", getSourceCodeFilesFromTar(it, setOf("java")))
    }
    val tex = latexTemplateEngine.process("source-code-listings", ctx)
    getPdf(tex, out)
  }

  private fun getSourceCodeFilesFromTar(tar: InputStream, extensions: Set<String>): List<SourceCodeView> {
    val tmpDir = createTempDir()
    TarUtil.extractTarToDirectory(tar, tmpDir)
    val files = tmpDir.walkTopDown().filter { file -> extensions.contains(file.extension) }
        .toList()
        .map { file -> SourceCodeView.fromFile(file, tmpDir) }
    tmpDir.deleteRecursively()
    return files
  }

  private fun getPdf(texSource: String, out: OutputStream) {
    val tmpDir = createTempDir()
    tmpDir.mkdirs()
    val texFile = File(tmpDir, "document.tex")
    texFile.writeText(texSource)
    val tar = ByteArrayOutputStream()
    TarUtil.createTarFromDirectory(tmpDir, tar)
    containerService.latexConvert(ByteArrayInputStream(tar.toByteArray()), texFile.name).use {
      TarUtil.extractTarToDirectory(it, tmpDir)
    }
    File(tmpDir, "document.pdf").inputStream()
        .afterClose { tmpDir.deleteRecursively() }
        .use { StreamUtils.copy(it, out) }
  }
}
