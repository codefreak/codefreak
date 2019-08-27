package de.code_freak.codefreak.util

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
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
    val tar = TarArchiveOutputStream(out)
    if (!file.isDirectory) {
      throw IllegalArgumentException("FileCollection must be a directory")
    }
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
      for (child in file.listFiles()) {
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
        tarEntry.size = zipEntry.size
        tar.putArchiveEntry(tarEntry)
        IOUtils.copy(zip, tar)
      }
      tar.closeArchiveEntry()
    }
    tar.finish()
  }

  private fun normalizeEntryName(name: String): String {
    if (name == ".") return ""
    return if (name.startsWith("./")) name.drop(2) else name
  }
}
