package de.code_freak.codefreak.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
import org.springframework.util.StreamUtils
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object TarUtil {
  fun extractTarToDirectory(`in`: InputStream, destination: File) {
    if (!destination.exists()) {
      destination.mkdirs()
    } else if (!destination.isDirectory) {
      throw IOException("${destination.absolutePath} already exists and is no directory")
    }
    val tar = TarArchiveInputStream(`in`)
    for (entry in generateSequence { tar.nextTarEntry }.filter { !it.isDirectory }) {
      val outFile = File(destination, entry.name)
      outFile.parentFile.mkdirs()
      outFile.outputStream().use { IOUtils.copy(tar, it) }
      outFile.setLastModified(entry.lastModifiedDate.time)
      // check if executable bit for user is set
      // octal 100 = dec 64
      outFile.setExecutable((entry.mode and 64) == 64)
    }
  }

  @Throws(IOException::class)
  fun checkValidTar(`in`: InputStream) {
    val tar = TarArchiveInputStream(`in`)
    generateSequence { tar.nextTarEntry }.forEach { _ -> /** Do nothing, just throw on error. */ }
  }

  fun createTarFromDirectory(file: File, out: OutputStream) {
    require(file.isDirectory) { "FileCollection must be a directory" }

    val tar = TarArchiveOutputStream(out)
    tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR)
    tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
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

  fun zipToTar(`in`: InputStream, out: OutputStream) {
    val zip = ZipArchiveInputStream(`in`)
    val tar = TarArchiveOutputStream(out)
    generateSequence { zip.nextZipEntry }.forEach { zipEntry ->
      val tarEntry = TarArchiveEntry(normalizeEntryName(zipEntry.name))
      if (zipEntry.isDirectory) {
        tar.putArchiveEntry(tarEntry)
      } else {
        val content = zip.readBytes()
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

  fun copyEntries(from: TarArchiveInputStream, to: TarArchiveOutputStream, filter: (TarArchiveEntry) -> Boolean = { true }) {
    generateSequence { from.nextTarEntry }
        .filter { filter(it) }
        .forEach { copyEntry(from, to, it) }
  }

  private fun copyEntry(from: TarArchiveInputStream, to: TarArchiveOutputStream, entry: TarArchiveEntry) {
    to.putArchiveEntry(entry)
    if (entry.isFile) {
      StreamUtils.copy(from, to)
    }
    to.closeArchiveEntry()
  }

  inline fun <reified T> getYamlDefinition(`in`: InputStream): T {
    TarArchiveInputStream(`in`).let { tar -> generateSequence { tar.nextTarEntry }.forEach {
      if (it.isFile && normalizeEntryName(it.name) == "codefreak.yml") {
        val mapper = ObjectMapper(YAMLFactory())
        return mapper.readValue(tar, T::class.java)
      }
    } }
    throw java.lang.IllegalArgumentException("codefreak.yml does not exist")
  }

  fun extractSubdirectory(`in`: InputStream, out: OutputStream, path: String) {
    val prefix = normalizeEntryName(path).withTrailingSlash()
    val extracted = TarArchiveOutputStream(out)
    TarArchiveInputStream(`in`).let { tar ->
      generateSequence { tar.nextTarEntry }.forEach {
        if (normalizeEntryName(it.name).startsWith(prefix)) {
          it.name = normalizeEntryName(it.name).drop(prefix.length)
          copyEntry(tar, extracted, it)
        }
      }
    }
  }

  fun processUploadedArchive(file: MultipartFile, out: OutputStream) {
    val filename = file.originalFilename ?: ""
    try {
      when {
        filename.endsWith(".tar", true) -> {
          file.inputStream.use { checkValidTar(it) }
          file.inputStream.use { StreamUtils.copy(it, out) }
        }
        filename.endsWith(".zip", true) -> {
          file.inputStream.use { zipToTar(it, out) }
        }
        else -> throw IllegalArgumentException("Unsupported file format")
      }
    } catch (e: IOException) {
      throw IllegalArgumentException("File could not be processed")
    }
  }
}
