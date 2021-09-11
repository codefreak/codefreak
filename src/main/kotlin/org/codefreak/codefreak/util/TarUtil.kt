package org.codefreak.codefreak.util

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.servlet.http.Part
import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveInputStream
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
import org.apache.commons.io.FilenameUtils
import org.springframework.util.StreamUtils

object TarUtil {
  const val CODEFREAK_DEFINITION_YML = "codefreak.yml"
  const val CODEFREAK_DEFINITION_YAML = "codefreak.yaml"

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
    val entry = TarArchiveEntry(file, normalizeFileName(name))
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
      if (tarEntry.isFile) {
        val zipEntry = ZipArchiveEntry(normalizeFileName(tarEntry.name))
        zipEntry.size = tarEntry.size
        zip.putArchiveEntry(zipEntry)
        IOUtils.copy(tar, zip)
      } else {
        // A ZipArchiveEntry is interpreted as a directory "if and only if it ends with a forward slash"
        val nameWithTrailingSlash = normalizeDirectoryName(tarEntry.name)
        val zipEntry = ZipArchiveEntry(nameWithTrailingSlash)
        zip.putArchiveEntry(zipEntry)
      }
      zip.closeArchiveEntry()
    }
    zip.finish()
  }

  fun tarToZip(tar: ByteArray): ByteArray {
    val out = ByteArrayOutputStream()
    tarToZip(ByteArrayInputStream(tar), out)
    return out.toByteArray()
  }

  fun isRoot(path: String) = normalizeFileName(path).isBlank()
  fun isRoot(entry: TarArchiveEntry) = isRoot(entry.name)

  private fun createTarRootDirectory(outputStream: TarArchiveOutputStream) {
    outputStream.putArchiveEntry(TarArchiveEntry("./"))
    outputStream.closeArchiveEntry()
  }

  @Throws(InvalidArchiveFormatException::class)
  fun archiveToTar(`in`: InputStream, out: OutputStream) {
    var input = BufferedInputStream(`in`)
    try {
      // try to read input as compressed type
      input = BufferedInputStream(CompressorStreamFactory().createCompressorInputStream(input))
    } catch (e: CompressorException) {
      // input is not compressed or maybe even not an archive at all
      // createArchiveInputStream() will fail if it's not an uncompressed archive
    }

    val archive: ArchiveInputStream
    try {
      archive = ArchiveStreamFactory().createArchiveInputStream(input)
    } catch (e: ArchiveException) {
      throw InvalidArchiveFormatException()
    }

    val tar = PosixTarArchiveOutputStream(out)
    createTarRootDirectory(tar)
    generateSequence { archive.nextEntry }
        // remove all dirs/files that are candidates for root b/c we created a root dir already
        .filter { !isRoot(it.name) }
        .forEach { archiveEntry ->
          val tarEntry = TarArchiveEntry(normalizeFileName(archiveEntry.name))
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

  fun copyEntries(
    from: TarArchiveInputStream,
    to: TarArchiveOutputStream,
    filter: (TarArchiveEntry) -> Boolean = { true }
  ) = copyEntries(from, to, filter, null)

  fun copyEntries(
    from: TarArchiveInputStream,
    to: TarArchiveOutputStream,
    filter: (TarArchiveEntry) -> Boolean = { true },
    prefix: String?
  ) {
    generateSequence { from.nextTarEntry }
        .filter(filter)
        .forEach {
          if (prefix != null) {
            it.name = "${normalizeFileName(prefix).withTrailingSlash()}${normalizeFileName(it.name)}"
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
    return findFile(`in`, listOf(path), consumer)
  }

  inline fun <T> findFile(`in`: InputStream, possiblePaths: List<String>, consumer: (TarArchiveEntry, TarArchiveInputStream) -> T): T {
    TarArchiveInputStream(`in`).let { tar ->
      generateSequence { tar.nextTarEntry }.forEach {
        possiblePaths.forEach { path ->
          if (it.isFile && normalizeFileName(it.name) == normalizeFileName(path)) {
            return consumer(it, tar)
          }
        }
      }
    }

    throw IllegalArgumentException("None of $possiblePaths does exist")
  }

  @Throws(IllegalArgumentException::class, InvalidCodefreakDefinitionException::class)
  inline fun <reified T> ObjectMapper.getCodefreakDefinition(`in`: InputStream): T {
    findFile(`in`, listOf(CODEFREAK_DEFINITION_YML, CODEFREAK_DEFINITION_YAML)) { _, fileStream ->
      try {
        return readValue(fileStream, T::class.java)
      } catch (e: JsonMappingException) {
        throw InvalidCodefreakDefinitionException(e.originalMessage)
      }
    }
  }

  fun isCodefreakDefinition(entry: TarArchiveEntry): Boolean {
    val normalizedName = normalizeFileName(entry.name)
    return normalizedName == CODEFREAK_DEFINITION_YML || normalizedName == CODEFREAK_DEFINITION_YAML
  }

  fun extractSubdirectory(`in`: InputStream, out: OutputStream, path: String) {
    val prefix = normalizeFileName(path).withTrailingSlash()
    val extracted = PosixTarArchiveOutputStream(out)
    TarArchiveInputStream(`in`).let { tar ->
      generateSequence { tar.nextTarEntry }.forEach {
        if (normalizeFileName(it.name).startsWith(prefix)) {
          it.name = normalizeFileName(it.name).drop(prefix.length)
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
      } catch (e: InvalidArchiveFormatException) {
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
    createEntryInTar(name, outputStream, EntryType.DIRECTORY)
  }

  private enum class EntryType(val linkFlag: Byte, val mode: Int) {
    FILE(TarConstants.LF_NORMAL, TarArchiveEntry.DEFAULT_FILE_MODE),
    DIRECTORY(TarConstants.LF_DIR, TarArchiveEntry.DEFAULT_DIR_MODE)
  }

  private fun createEntryInTar(name: String, outputStream: TarArchiveOutputStream, type: EntryType, content: InputStream? = null) {
    val contentBytes = content?.readBytes()

    TarArchiveEntry(name, type.linkFlag, false).also {
      it.mode = type.mode

      contentBytes?.let { bytes ->
        it.size = bytes.size.toLong()
      }

      outputStream.putArchiveEntry(it)

      contentBytes?.let { bytes ->
        outputStream.write(bytes)
      }

      outputStream.closeArchiveEntry()
    }
  }

  private fun basename(path: String): String {
    path.split("[\\\\/]".toRegex()).apply {
      return if (isEmpty()) "" else last()
    }
  }

  fun touch(name: String, outputStream: TarArchiveOutputStream) {
    createEntryInTar(name, outputStream, EntryType.FILE)
  }

  fun normalizeFileName(name: String) = FilenameUtils.separatorsToUnix(FileUtil.normalizePath(name)).withoutTrailingSlash().trimStart('/')
  fun normalizeDirectoryName(name: String) = FilenameUtils.separatorsToUnix(FileUtil.normalizePath(name)).withTrailingSlash().trimStart('/')
  fun getParentDir(name: String) = FilenameUtils.separatorsToUnix(FileUtil.getParentDir(name)).trimStart('/')
  fun getParentDirs(name: String) = FileUtil.getParentDirs(name).map { FilenameUtils.separatorsToUnix(it).trimStart('/') }

  fun TarArchiveInputStream.entrySequence() = generateSequence { nextTarEntry }

  fun writeFileWithContent(name: String, content: InputStream, outputStream: TarArchiveOutputStream) {
    createEntryInTar(name, outputStream, EntryType.FILE, content)
  }
}
