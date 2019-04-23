package de.code_freak.codefreak.util

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException

object TarUtil {
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
    tar.putArchiveEntry(TarArchiveEntry(file, name))

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
}
