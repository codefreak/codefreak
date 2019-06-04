package de.code_freak.codefreak.util

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object TarUtil {
  fun extractTarToDirectory(tar: ByteArray, destination: File) {
    if (!destination.exists()) {
      destination.mkdirs()
    } else if (!destination.isDirectory) {
      throw IOException("${destination.absolutePath} already exists and is no directory")
    }
    val tarInputStream = TarArchiveInputStream(tar.inputStream())
    for (entry in generateSequence { tarInputStream.nextTarEntry }.filter { !it.isDirectory }) {
      val outFile = File(destination, entry.name)
      outFile.parentFile.mkdirs()
      IOUtils.copy(tarInputStream, outFile.outputStream())
    }
  }

  fun createTarFromDirectory(file: File): ByteArray {
    if (!file.isDirectory) {
      throw IllegalArgumentException("File must be a directory")
    }
    val outputStream = ByteArrayOutputStream()
    val tar = TarArchiveOutputStream(outputStream)
    tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR)
    tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
    addFileToTar(tar, file, ".")
    tar.close()
    return outputStream.toByteArray()
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
      for (child in file.listFiles()) {
        addFileToTar(tar, child, "$name/${child.name}")
      }
    }
  }

  fun tarToZip(tarContent: ByteArray): ByteArray {
    val tar = TarArchiveInputStream(ByteArrayInputStream(tarContent))
    val zipContent = ByteArrayOutputStream()
    val zip = ZipArchiveOutputStream(zipContent)
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
    return zipContent.toByteArray()
  }

  private fun normalizeEntryName(name: String): String {
    if (name == ".") return ""
    return if (name.startsWith("./")) name.drop(2) else name
  }
}
