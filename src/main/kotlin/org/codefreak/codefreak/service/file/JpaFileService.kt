package org.codefreak.codefreak.service.file

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
import org.codefreak.codefreak.entity.FileCollection
import org.codefreak.codefreak.repository.FileCollectionRepository
import org.codefreak.codefreak.service.EntityNotFoundException
import org.codefreak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.util.DigestUtils

@Service
@ConditionalOnProperty(name = ["codefreak.files.adapter"], havingValue = "JPA")
class JpaFileService : FileService {

  @Autowired
  private lateinit var fileCollectionRepository: FileCollectionRepository

  override fun writeCollectionTar(collectionId: UUID): OutputStream {
    val collection = fileCollectionRepository.findById(collectionId).orElseGet { FileCollection(collectionId) }
    return object : ByteArrayOutputStream() {
      override fun close() {
        collection.tar = toByteArray()
        fileCollectionRepository.save(collection)
      }
    }
  }

  protected fun getCollection(collectionId: UUID): FileCollection = fileCollectionRepository.findById(collectionId)
      .orElseThrow { EntityNotFoundException("File not found") }

  override fun readCollectionTar(collectionId: UUID): InputStream {
    return ByteArrayInputStream(getCollection(collectionId).tar)
  }

  override fun collectionExists(collectionId: UUID): Boolean {
    return fileCollectionRepository.existsById(collectionId)
  }

  override fun deleteCollection(collectionId: UUID) {
    fileCollectionRepository.deleteById(collectionId)
  }

  override fun getCollectionMd5Digest(collectionId: UUID): ByteArray {
    return DigestUtils.md5Digest(getCollection(collectionId).tar)
  }

  override fun createFile(collectionId: UUID, path: String) {
    val normalizedPath = TarUtil.normalizeFileName(path)

    requireValidPath(normalizedPath)
    requireFileDoesNotExist(collectionId, normalizedPath)

    getTarOutputStream(collectionId).use {
      val input = getTarInputStream(collectionId)
      TarUtil.copyEntries(input, it)
      TarUtil.touch(normalizedPath, it)
    }
  }

  private fun requireValidPath(path: String) = require(path.isNotBlank()) { "`$path` is not a valid path" }
  private fun requireFileDoesExist(collectionId: UUID, path: String) =
    require(containsFile(collectionId, path)) { "File `$path` does not exist" }

  private fun requireFileDoesNotExist(collectionId: UUID, path: String) =
    require(!containsFile(collectionId, path)) { "File `$path` already exists" }

  private fun requireDirectoryDoesExist(collectionId: UUID, path: String) =
    require(containsDirectory(collectionId, path)) { "Directory `$path` does not exist" }

  private fun requireDirectoryDoesNotExist(collectionId: UUID, path: String) =
    require(!containsDirectory(collectionId, path)) { "Directory `$path` already exists" }

  override fun containsFile(collectionId: UUID, path: String): Boolean {
    val normalizedPath = TarUtil.normalizeFileName(path)
    return containsPath(collectionId, normalizedPath) { it.isFile }
  }

  override fun containsDirectory(collectionId: UUID, path: String): Boolean {
    val normalizedPath = TarUtil.normalizeDirectoryName(path)
    return containsPath(collectionId, normalizedPath) { it.isDirectory }
  }

  private fun containsPath(
    collectionId: UUID,
    path: String,
    restriction: (TarArchiveEntry) -> Boolean = { true }
  ): Boolean = findEntry(collectionId, path, restriction) != null

  private fun findEntry(
    collectionId: UUID,
    path: String,
    restriction: (TarArchiveEntry) -> Boolean = { true }
  ): TarArchiveEntry? {
    getTarInputStream(collectionId).use {
      generateSequence { it.nextTarEntry }.forEach { entry ->
        if (entry.name == path && restriction(entry)) {
          return entry
        }
      }
    }
    return null
  }

  private fun getTarInputStream(collectionId: UUID) = TarArchiveInputStream(readCollectionTar(collectionId))

  private fun getTarOutputStream(collectionId: UUID) = TarArchiveOutputStream(writeCollectionTar(collectionId))

  override fun createDirectory(collectionId: UUID, path: String) {
    val normalizedPath = TarUtil.normalizeDirectoryName(path)

    requireValidPath(normalizedPath)
    requireDirectoryDoesNotExist(collectionId, normalizedPath)

    getTarOutputStream(collectionId).use {
      val input = getTarInputStream(collectionId)
      TarUtil.copyEntries(input, it)
      TarUtil.mkdir(normalizedPath, it)
    }
  }

  override fun deleteFile(collectionId: UUID, path: String) {
    requireValidPath(TarUtil.normalizeEntryName(path))
    val fileExists = containsFile(collectionId, TarUtil.normalizeFileName(path))
    val directoryExists = containsDirectory(collectionId, TarUtil.normalizeDirectoryName(path))
    require(fileExists.or(directoryExists)) { "`$path` does not exist" }

    val normalizedPath = if (fileExists) TarUtil.normalizeFileName(path) else TarUtil.normalizeDirectoryName(path)

    getTarOutputStream(collectionId).use {
      val input = getTarInputStream(collectionId)
      TarUtil.copyEntries(input, it, { entry -> !entry.name.startsWith(normalizedPath) })
    }
  }

  override fun filePutContents(collectionId: UUID, path: String): OutputStream {
    val normalizedPath = TarUtil.normalizeFileName(path)

    requireValidPath(normalizedPath)
    requireFileDoesExist(collectionId, normalizedPath)

    return object : ByteArrayOutputStream() {
      override fun close() {
        val contents = toByteArray()
        val entryToPut = findEntry(collectionId, normalizedPath)!!
        entryToPut.size = contents.size.toLong()

        getTarOutputStream(collectionId).use {
          val tar = getTarInputStream(collectionId)
          TarUtil.copyEntries(tar, it, { entry -> entry.name != normalizedPath })

          it.putArchiveEntry(entryToPut)
          IOUtils.copy(contents.inputStream(), it)
          it.closeArchiveEntry()
        }
      }
    }
  }

  override fun getFileContents(collectionId: UUID, path: String): InputStream {
    val normalizedPath = TarUtil.normalizeFileName(path)

    requireValidPath(normalizedPath)
    requireFileDoesExist(collectionId, normalizedPath)

    getTarInputStream(collectionId).use {
      generateSequence { it.nextTarEntry }.forEach { entry ->
        if (entry.name == normalizedPath) {
          val output = ByteArrayOutputStream()
          IOUtils.copy(it, output)
          return ByteArrayInputStream(output.toByteArray())
        }
      }
    }

    return ByteArrayInputStream(byteArrayOf())
  }

  override fun moveFile(collectionId: UUID, from: String, to: String) {
    var normalizedFrom = TarUtil.normalizeEntryName(from)
    var normalizedTo = TarUtil.normalizeEntryName(to)

    requireValidPath(normalizedFrom)
    requireValidPath(normalizedTo)

    val isDirectory = containsDirectory(collectionId, TarUtil.normalizeDirectoryName(from))
    if (isDirectory) {
      normalizedFrom = TarUtil.normalizeDirectoryName(from)
      normalizedTo = TarUtil.normalizeDirectoryName(to)

      requireDirectoryDoesExist(collectionId, normalizedFrom)
      requireDirectoryDoesNotExist(collectionId, normalizedTo)
    } else {
      requireFileDoesExist(collectionId, normalizedFrom)
      requireFileDoesNotExist(collectionId, normalizedTo)
    }

    moveFileOrDirectory(collectionId, normalizedFrom, normalizedTo)
  }

  private fun moveFileOrDirectory(collectionId: UUID, from: String, to: String) {
    getTarOutputStream(collectionId).use { tar ->
      getTarInputStream(collectionId).use { input ->
        generateSequence { input.nextTarEntry }.forEach { entry ->
          if (entry.name.startsWith(from)) {
            entry.name = entry.name.replace(from, to)
          }

          tar.putArchiveEntry(entry)
          IOUtils.copy(input, tar)
          tar.closeArchiveEntry()
        }
      }
    }
  }
}
