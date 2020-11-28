package org.codefreak.codefreak.service.file

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
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
import java.util.UUID

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

    require(normalizedPath.isNotBlank())
    require(!containsFile(collectionId, normalizedPath))

    getTarOutputStream(collectionId).use {
      val input = getTarInputStream(collectionId)
      TarUtil.copyEntries(input, it)
      TarUtil.touch(normalizedPath, it)
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
    val normalizedPath = TarUtil.normalizeEntryName(path).withTrailingSlash()

    require(normalizedPath.isNotBlank())
    require(!containsDirectory(collectionId, normalizedPath))

    getTarOutputStream(collectionId).use {
      val input = getTarInputStream(collectionId)
      TarUtil.copyEntries(input, it)
      TarUtil.mkdir(normalizedPath, it)
    }
  }

  override fun deleteFile(collectionId: UUID, path: String) {
    var normalizedPath = TarUtil.normalizeEntryName(path)

    require(normalizedPath.isNotBlank())
    val fileExists = containsFile(collectionId, normalizedPath.withoutTrailingSlash())
    val directoryExists = containsDirectory(collectionId, normalizedPath.withoutTrailingSlash())
    require(fileExists.or(directoryExists))

    normalizedPath = if (fileExists) normalizedPath.withoutTrailingSlash() else normalizedPath.withTrailingSlash()

    getTarOutputStream(collectionId).use {
      val input = getTarInputStream(collectionId)
      TarUtil.copyEntries(input, it, { entry -> !entry.name.startsWith(normalizedPath) })
    }
  }

  override fun filePutContents(collectionId: UUID, path: String): OutputStream {
    val normalizedPath = TarUtil.normalizeEntryName(path).withoutTrailingSlash()

    require(normalizedPath.isNotBlank())
    require(containsFile(collectionId, normalizedPath))

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
    val normalizedPath = TarUtil.normalizeEntryName(path).withoutTrailingSlash()

    require(normalizedPath.isNotBlank())
    require(containsFile(collectionId, normalizedPath))

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
    val normalizedFrom = TarUtil.normalizeEntryName(from).withoutTrailingSlash()
    val normalizedTo = TarUtil.normalizeEntryName(to).withoutTrailingSlash()

    require(normalizedFrom.isNotBlank())
    require(normalizedTo.isNotBlank())
    require(containsFile(collectionId, normalizedFrom))
    require(!containsFile(collectionId, normalizedTo))

    val contents = getFileContents(collectionId, normalizedFrom)
    createFile(collectionId, normalizedTo)
    filePutContents(collectionId, normalizedTo).use {
      it.write(contents.readBytes())
    }
    deleteFile(collectionId, normalizedFrom)
  }

  override fun moveDirectory(collectionId: UUID, from: String, to: String) {
    val normalizedFrom = TarUtil.normalizeEntryName(from).withTrailingSlash()
    val normalizedTo = TarUtil.normalizeEntryName(to).withTrailingSlash()

    require(normalizedFrom.isNotBlank())
    require(normalizedTo.isNotBlank())
    require(containsDirectory(collectionId, normalizedFrom))
    require(!containsDirectory(collectionId, normalizedTo))

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
    deleteFile(collectionId, normalizedFrom)
  }

  private fun findDirectoryChildren(collectionId: UUID, path: String): Map<String, InputStream?> {
    val children = mutableMapOf<String, InputStream?>()

    getTarInputStream(collectionId).use {
      generateSequence { it.nextTarEntry }.forEach { child ->
        if (child.name.startsWith(path) && child.name != path) {
          children[child.name] = if (child.isFile) {
            getFileContents(collectionId, child.name)
          } else {
            null
          }
        }
      }
    }

    return children
  }
}
