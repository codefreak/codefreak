package org.codefreak.codefreak.service.file

import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import org.springframework.util.DigestUtils
import org.springframework.util.StreamUtils

interface FileService {
  fun readCollectionTar(collectionId: UUID): InputStream
  fun writeCollectionTar(collectionId: UUID): OutputStream
  fun collectionExists(collectionId: UUID): Boolean
  fun deleteCollection(collectionId: UUID)

  fun copyCollection(oldId: UUID, newId: UUID) {
    readCollectionTar(oldId).use {
      writeCollectionTar(newId).use { out -> StreamUtils.copy(it, out) }
    }
  }

  fun getCollectionMd5Digest(collectionId: UUID): ByteArray {
    return readCollectionTar(collectionId).use { DigestUtils.md5Digest(it) }
  }

  fun createFile(collectionId: UUID, path: String)
  fun createDirectory(collectionId: UUID, path: String)
  fun containsFile(collectionId: UUID, path: String): Boolean
  fun containsDirectory(collectionId: UUID, path: String): Boolean
  fun deleteFile(collectionId: UUID, path: String)
  fun deleteDirectory(collectionId: UUID, path: String)
  fun filePutContents(collectionId: UUID, path: String, contents: ByteArray)
  fun getFileContents(collectionId: UUID, path: String): ByteArray
  fun moveFile(collectionId: UUID, from: String, to: String)
  fun moveDirectory(collectionId: UUID, from: String, to: String)
}
