package org.codefreak.codefreak.service.file

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
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
      throw IllegalArgumentException("$path does not exist or is no file")
    }
  }

  private fun requireFileDoesNotExist(collectionId: UUID, path: String) {
    if (containsFile(collectionId, path)) {
      throw IllegalArgumentException("$path already exists")
    }
  }

  private fun requireDirectoryDoesExist(collectionId: UUID, path: String) {
    if (!containsDirectory(collectionId, path)) {
      throw IllegalArgumentException("$path does not exist or is no directory")
    }
  }

  private fun requireDirectoryDoesNotExist(collectionId: UUID, path: String) {
    if (containsDirectory(collectionId, path)) {
      throw IllegalArgumentException("$path already exists")
    }
  }

  override fun containsFile(collectionId: UUID, path: String): Boolean {
    val normalizedPath = TarUtil.normalizeEntryName(path).withoutTrailingSlash()
    return containsPath(collectionId, normalizedPath) { it.isFile }
  }

  override fun containsDirectory(collectionId: UUID, path: String): Boolean {
    val normalizedPath = TarUtil.normalizeEntryName(path).withTrailingSlash()
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
      do {
        val entry = it.nextTarEntry
        if (entry?.name == path && restriction(entry)) {
          return entry
        }
      } while (entry != null)
    }

    return null
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
    val normalizedPath = TarUtil.normalizeEntryName(path).withoutTrailingSlash()

    requireValidPattern(normalizedPath)
    requireFileDoesExist(collectionId, normalizedPath)

    getTarOutputStream(collectionId).use {
      val input = getTarInputStream(collectionId)
      TarUtil.copyEntries(input, it, { entry -> entry.name != normalizedPath })
    }
  }

  override fun deleteDirectory(collectionId: UUID, path: String) {
    val normalizedPath = TarUtil.normalizeEntryName(path).withTrailingSlash()

    requireValidPattern(normalizedPath)
    requireDirectoryDoesExist(collectionId, normalizedPath)

    getTarOutputStream(collectionId).use {
      val input = getTarInputStream(collectionId)
      TarUtil.copyEntries(input, it, { entry -> !entry.name.startsWith(normalizedPath) })
    }
  }

  override fun filePutContents(collectionId: UUID, path: String, contents: ByteArray) {
    val normalizedPath = TarUtil.normalizeEntryName(path).withoutTrailingSlash()

    requireValidPattern(normalizedPath)
    requireFileDoesExist(collectionId, normalizedPath)

    val entryToPut = findEntry(collectionId, normalizedPath)!!
    entryToPut.size = contents.size.toLong()

    getTarOutputStream(collectionId).use {
      val tar = getTarInputStream(collectionId)
      TarUtil.copyEntries(tar, it, { entry -> entry.name != normalizedPath })

      val contentsInput = ByteArrayInputStream(contents)
      it.putArchiveEntry(entryToPut)
      IOUtils.copy(contentsInput, it)
      it.closeArchiveEntry()
    }
  }

  override fun getFileContents(collectionId: UUID, path: String): ByteArray {
    val normalizedPath = TarUtil.normalizeEntryName(path).withoutTrailingSlash()

    requireValidPattern(normalizedPath)
    requireFileDoesExist(collectionId, normalizedPath)

    val input = getTarInputStream(collectionId)
    do {
      val entry = input.nextTarEntry
      entry?.let {
        if (it.name == normalizedPath) {
          val output = ByteArrayOutputStream()
          IOUtils.copy(input, output)
          return output.toByteArray()
        }
      }
    } while (entry != null)

    return byteArrayOf()
  }

  override fun moveFile(collectionId: UUID, from: String, to: String) {
    val normalizedFrom = TarUtil.normalizeEntryName(from).withoutTrailingSlash()
    val normalizedTo = TarUtil.normalizeEntryName(to).withoutTrailingSlash()

    requireValidPattern(normalizedFrom)
    requireValidPattern(normalizedTo)
    requireFileDoesExist(collectionId, normalizedFrom)
    requireFileDoesNotExist(collectionId, normalizedTo)

    val contents = getFileContents(collectionId, normalizedFrom)
    createFile(collectionId, normalizedTo)
    filePutContents(collectionId, normalizedTo, contents)
    deleteFile(collectionId, normalizedFrom)
  }

  override fun moveDirectory(collectionId: UUID, from: String, to: String) {
    val normalizedFrom = TarUtil.normalizeEntryName(from).withTrailingSlash()
    val normalizedTo = TarUtil.normalizeEntryName(to).withTrailingSlash()

    requireValidPattern(normalizedFrom)
    requireValidPattern(normalizedTo)
    requireDirectoryDoesExist(collectionId, normalizedFrom)
    requireDirectoryDoesNotExist(collectionId, normalizedTo)

    val children = findDirectoryChildren(collectionId, normalizedFrom)

    children.forEach {
      val newPath = it.key.replace(normalizedFrom, normalizedTo)
      val isDirectory = it.value == null

      if (isDirectory) {
        createDirectory(collectionId, newPath)
      } else {
        moveFile(collectionId, it.key, newPath)
      }
    }

    createDirectory(collectionId, normalizedTo)
    deleteDirectory(collectionId, normalizedFrom)
  }

  private fun findDirectoryChildren(collectionId: UUID, path: String): Map<String, ByteArray?> {
    val children = mutableMapOf<String, ByteArray?>()

    getTarInputStream(collectionId).use {
      do {
        val child = it.nextTarEntry
        child?.let {
          if (child.name.startsWith(path) && child.name != path) {
            children[child.name] = if (child.isFile) {
              getFileContents(collectionId, child.name)
            } else {
              null
            }
          }
        }
      } while (child != null)
    }

    return children
  }
}
