package de.code_freak.codefreak.service.file

import org.springframework.util.StreamUtils
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

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
}
