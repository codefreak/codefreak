package de.code_freak.codefreak.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.compressors.CompressorException
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.utils.IOUtils
import org.springframework.util.StreamUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.servlet.http.Part

object TarUtil {
  class PosixTarArchiveOutputStream(out: OutputStream) : TarArchiveOutputStream(out) {
    init {
      setLongFileMode(LONGFILE_POSIX)
      setBigNumberMode(BIGNUMBER_STAR)
    }
  }

  fun createTarFromDirectory(file: File, out: OutputStream) {
    require(file.isDirectory) { "FileCollection must be a directory" }

    val tar = PosixTarArchiveOutputStream(out)
    addFileToTar(tar, file, ".")
    tar.finish()
  }

  private fun addFileToTar(tar: TarArchiveOutputStream, file: File, name: String) {
    val entry = TarArchiveEntry(file, normalizeEntryName(name))
    // add the executable bit for user. Default mode is 0644
    // 0644 + 0100 = 0744
    if (file.isFile && file.canExecute()) {
      entry.mode += 64 // 0100
    }

    tar.putArchiveEntry(entry)

    if (file.isFile) {
      BufferedInputStream(FileInputStream(file)).use {
        IOUtils.copy(it, tar)
      }
      tar.closeArchiveEntry()
    } else if (file.isDirectory) {
      tar.closeArchiveEntry()
      for (child in file.listFiles() ?: emptyArray()) {
        addFileToTar(tar, child, "$name/${child.name}")
      }
    }
  }

  fun tarToZip(`in`: InputStream, out: OutputStream) {
    val tar = TarArchiveInputStream(`in`)
    val zip = ZipArchiveOutputStream(out)
    generateSequence { tar.nextTarEntry }.forEach { tarEntry ->
      val zipEntry = ZipArchiveEntry(normalizeEntryName(tarEntry.name))
      if (tarEntry.isFile) {
        zipEntry.size = tarEntry.size
        zip.putArchiveEntry(zipEntry)
        IOUtils.copy(tar, zip)
      } else {
        zip.putArchiveEntry(zipEntry)
      }
      zip.closeArchiveEntry()
    }
    zip.finish()
  }

  /**
   * Check if given path name is the name of a root file like "/", "./", "." or even
   * weird combinations like "../../."
   */
  private fun isRootFilename(path: String) = path.trim('/', '.').isBlank()

  private fun createTarRootDirectory(outputStream: TarArchiveOutputStream) {
    outputStream.putArchiveEntry(TarArchiveEntry("./"))
    outputStream.closeArchiveEntry()
  }

  fun archiveToTar(`in`: InputStream, out: OutputStream) {
    var input = BufferedInputStream(`in`)
    try {
      // try to read input as compressed type
      input = BufferedInputStream(CompressorStreamFactory().createCompressorInputStream(input))
    } catch (e: CompressorException) {
      // input is not compressed or maybe even not an archive at all
      // createArchiveInputStream() will fail if it's not an uncompressed archive
    }
    val archive = ArchiveStreamFactory().createArchiveInputStream(input)
    val tar = PosixTarArchiveOutputStream(out)
    createTarRootDirectory(tar)
    generateSequence { archive.nextEntry }
        // remove all dirs/files that are candidates for root b/c we created a root dir already
        .filter { !isRootFilename(it.name) }
        .forEach { archiveEntry ->
          val tarEntry = TarArchiveEntry(normalizeEntryName(archiveEntry.name))
          if (archiveEntry.isDirectory) {
            tar.putArchiveEntry(tarEntry)
          } else {
            val content = archive.readBytes()
            tarEntry.size = content.size.toLong()
            tar.putArchiveEntry(tarEntry)
            tar.write(content)
          }
          tar.closeArchiveEntry()
        }
    tar.finish()
  }

  fun normalizeEntryName(name: String): String {
    if (name == ".") return ""
    return if (name.startsWith("./")) name.drop(2) else name
  }

  fun copyEntries(
    from: InputStream,
    to: OutputStream,
    filter: (TarArchiveEntry) -> Boolean = { true },
    prefix: String? = null
  ) = copyEntries(TarArchiveInputStream(from), PosixTarArchiveOutputStream(to), filter, prefix)

  fun copyEntries(
    from: TarArchiveInputStream,
    to: TarArchiveOutputStream,
    filter: (TarArchiveEntry) -> Boolean = { true },
    prefix: String? = null
  ) {
    generateSequence { from.nextTarEntry }
        .filter(filter)
        .forEach {
          if (prefix != null) {
            it.name = "${normalizeEntryName(prefix).withTrailingSlash()}${normalizeEntryName(it.name)}"
          }
          copyEntry(from, to, it)
        }
  }

  private fun copyEntry(from: TarArchiveInputStream, to: TarArchiveOutputStream, entry: TarArchiveEntry) {
    to.putArchiveEntry(entry)
    if (entry.isFile) {
      StreamUtils.copy(from, to)
    }
    to.closeArchiveEntry()
  }

  inline fun <T> findFile(`in`: InputStream, path: String, consumer: (TarArchiveEntry, TarArchiveInputStream) -> T): T {
    TarArchiveInputStream(`in`).let { tar ->
      generateSequence { tar.nextTarEntry }.forEach {
        if (it.isFile && normalizeEntryName(it.name) == normalizeEntryName(path)) {
          return consumer(it, tar)
        }
      }
    }
    throw IllegalArgumentException("$path does not exist")
  }

  @Throws(IllegalArgumentException::class)
  inline fun <reified T> getYamlDefinition(`in`: InputStream): T {
    findFile(`in`, "codefreak.yml") { _, fileStream ->
      val mapper = ObjectMapper(YAMLFactory())
      return mapper.readValue(fileStream, T::class.java)
    }
  }

  fun extractSubdirectory(`in`: InputStream, out: OutputStream, path: String) {
    val prefix = normalizeEntryName(path).withTrailingSlash()
    val extracted = PosixTarArchiveOutputStream(out)
    TarArchiveInputStream(`in`).let { tar ->
      generateSequence { tar.nextTarEntry }.forEach {
        if (normalizeEntryName(it.name).startsWith(prefix)) {
          it.name = normalizeEntryName(it.name).drop(prefix.length)
          copyEntry(tar, extracted, it)
        }
      }
    }
  }

  fun writeUploadAsTar(files: Array<out Part>, out: OutputStream) {
    if (files.size == 1) {
      try {
        // try to read upload as archive
        return files[0].inputStream.use { archiveToTar(it, out) }
      } catch (e: ArchiveException) {
        // unknown archive type or no archive at all
        // create a new tar archive that contains only the uploaded file
      }
    }
    wrapUploadInTar(files, out)
  }

  private fun wrapUploadInTar(files: Array<out Part>, out: OutputStream) {
    val tar = PosixTarArchiveOutputStream(out)
    createTarRootDirectory(tar)
    for (file in files) {
      val entry = TarArchiveEntry(basename(file.submittedFileName ?: UUID.randomUUID().toString()))
      entry.size = file.size
      tar.putArchiveEntry(entry)
      file.inputStream.use { StreamUtils.copy(it, tar) }
      tar.closeArchiveEntry()
    }
    tar.finish()
  }

  fun mkdir(name: String, outputStream: TarArchiveOutputStream) {
    TarArchiveEntry(name, TarConstants.LF_DIR, false).also {
      it.mode = TarArchiveEntry.DEFAULT_DIR_MODE
      outputStream.putArchiveEntry(it)
      outputStream.closeArchiveEntry()
    }
  }

  private fun basename(path: String): String {
    path.split("[\\\\/]".toRegex()).apply {
      return if (isEmpty()) "" else last()
    }
  }
}
