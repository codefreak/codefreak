package org.codefreak.codefreak.service.file

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.util.UUID
import org.codefreak.codefreak.entity.FileCollection
import org.codefreak.codefreak.repository.FileCollectionRepository
import org.codefreak.codefreak.service.EntityNotFoundException
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.withTrailingSlash
import org.codefreak.codefreak.util.withoutTrailingSlash
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.util.DigestUtils
import java.io.*
import java.lang.IllegalArgumentException

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
    val normalizedPath = TarUtil.normalizeEntryName(path).withoutTrailingSlash()

    requireValidPattern(path)
    requireFileDoesNotExist(collectionId, normalizedPath)

    getTarOutputStream(collectionId).use {
      val input = getTarInputStream(collectionId)
      TarUtil.copyEntries(input, it)
      TarUtil.touch(normalizedPath, it)
    }
  }

  private fun requireValidPattern(path: String) {
    if (path.isBlank()) {
      throw IllegalArgumentException("$path is not a valid path pattern")
    }
  }

  private fun requireFileDoesExist(collectionId: UUID, path: String) {
    if (!containsFile(collectionId, path)) {
      throw IllegalArgumentException("$path does not exist")
    }
  }

  private fun requireFileDoesNotExist(collectionId: UUID, path: String) {
    if (containsFile(collectionId, path)) {
      throw IllegalArgumentException("$path already exists")
    }
  }

  private fun requireDirectoryDoesNotExist(collectionId: UUID, path: String) {
    if (containsDirectory(collectionId, path)) {
      throw IllegalArgumentException("$path already exists")
    }
  }

  fun containsFile(collectionId: UUID, path: String): Boolean {
    val normalizedPath = TarUtil.normalizeEntryName(path).withoutTrailingSlash()
    return containsPath(collectionId, normalizedPath) { it.isFile }
  }

  fun containsDirectory(collectionId: UUID, path: String): Boolean {
    val normalizedPath = TarUtil.normalizeEntryName(path).withTrailingSlash()
    return containsPath(collectionId, normalizedPath) { it.isDirectory }
  }

  private fun containsPath(
      collectionId: UUID,
      path: String,
      restriction: (TarArchiveEntry) -> Boolean = { true }
  ): Boolean {
    getTarInputStream(collectionId).use {
      do {
        val entry = it.nextTarEntry
        if (entry?.name == path && restriction(entry)) {
          return true
        }
      } while (entry != null)
    }

    return false
  }

  private fun getTarInputStream(collectionId: UUID) = TarArchiveInputStream(readCollectionTar(collectionId))

  private fun getTarOutputStream(collectionId: UUID) = TarArchiveOutputStream(writeCollectionTar(collectionId))

  override fun createDirectory(collectionId: UUID, path: String) {
    val normalizedPath = TarUtil.normalizeEntryName(path).withTrailingSlash()

    requireValidPattern(normalizedPath)
    requireDirectoryDoesNotExist(collectionId, normalizedPath)

    getTarOutputStream(collectionId).use {
      val input = getTarInputStream(collectionId)
      TarUtil.copyEntries(input, it)
      TarUtil.mkdir(normalizedPath, it)
    }
  }

  override fun deleteFile(collectionId: UUID, path: String) {
    val normalizedPath = TarUtil.normalizeEntryName(path)

    requireFileDoesExist(collectionId, normalizedPath)

    getTarOutputStream(collectionId).use {
      val input = getTarInputStream(collectionId)
      TarUtil.copyEntries(input, it, { entry -> entry.name != normalizedPath })
    }
  }
}
