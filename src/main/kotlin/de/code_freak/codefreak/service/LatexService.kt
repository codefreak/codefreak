package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.util.TarUtil
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class LatexService {
  companion object {
    val LATEX_SPECIAL_CHARS = listOf("_", "^", "~", "$", "%", "#", "&", "{", "}")
    fun escapeString(input: String): String {
      return LATEX_SPECIAL_CHARS.fold(input) { text, char ->
        return text.replace(char, "\\$char")
      }
    }
  }

  fun answerToPdf(answer: Answer): ByteArray {
    val tmpDir = createTempDir()
    TarUtil.extractTarToDirectory(answer.files!!, tmpDir)
    val pdf = getPdf(
        SourceCodeDocument.fromDirectory(tmpDir, listOf("java"))
    )
    tmpDir.deleteRecursively()
    return pdf
  }

  /**
   * TODO: use Docker container so you do not have to install latex
   */
  fun getPdf(document: Document): ByteArray {
    val tex = document.getTexSource()
    val tmpDir = createTempDir()
    tmpDir.mkdirs()
    val texFile = File(tmpDir, "document.tex")
    texFile.writeText(tex)
    val proc = ProcessBuilder("/usr/bin/xelatex -synctex=1 -interaction=nonstopmode document.tex".split("\\s+".toRegex()))
        .directory(tmpDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    val exit = proc.waitFor()
    if(exit == 1) {
      val errorText = proc.inputStream.bufferedReader().readText()
      throw Exception("Failed to tex pdf in ${tmpDir.path}:\n" + errorText)
    }

    val pdfContent = File(tmpDir, "document.pdf").readBytes()
    tmpDir.deleteRecursively()
    return pdfContent
  }

  interface Document {
    fun getTexSource(): String
  }

  class SourceCodeDocument(val files: List<File>, val baseDirectory: File) : Document {
    val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    companion object {
      fun fromDirectory(dir: File, extensions: List<String> = listOf("java")): SourceCodeDocument {
        return SourceCodeDocument(
            dir.walkTopDown().filter { file -> extensions.contains(file.extension) }.toList(),
            dir
        )
      }
    }

    override fun getTexSource(): String {
      return getHeader() + "\n" + getBody() + "\n" + getFooter()
    }

    protected fun getBody(): String {
      var body = ""
      val last = files.lastIndex
      files.forEachIndexed { index, file ->
        body += generateSourceFileTex(file)
        if (index != last) {
          body += "\n\\pagebreak"
        }
      }
      return body
    }

    private fun generateSourceFileTex(file: File): String {
      val path = LatexService.escapeString(file.relativeTo(baseDirectory).path)
      // TODO: escape content
      val content = file.readText()
      val modified = Instant.ofEpochMilli(file.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime().format(dateFormat)
      return """
\begin{table}[h]
\begin{tabularx}{\textwidth}{rX}
  \textbf{Filename} & \texttt{${path}} \\
  \textbf{Last modified} & ${modified} \\
  \hline
\end{tabularx}
\end{table}
\begin{lstlisting}[language=${file.extension}]
$content
\end{lstlisting}
      """
    }

    protected fun getFooter(): String {
      return "\\end{document}"
    }

    protected fun getHeader(): String {
      return """
\documentclass[10pt,a4paper]{article}
\usepackage[T1]{fontenc}
\usepackage[margin=0.5in]{geometry}
\usepackage{tabularx}
\usepackage{listings}
\usepackage{xcolor}

\lstset{
  extendedchars=true,
  showstringspaces=false,
  aboveskip=10pt,
  showspaces=false,
  numbers=left,
  numbersep=9pt,
  tabsize=2,
  breaklines=true,
  showtabs=false,
  captionpos=b,
  escapeinside={\%(*}{*)},
  numberstyle=\footnotesize,
  basicstyle=\footnotesize\ttfamily,
  backgroundcolor=\color{white},
  commentstyle=\color{olive},
  keywordstyle=\color{blue},
  stringstyle=\color{magenta},
}

\begin{document}
      """
    }
  }
}
