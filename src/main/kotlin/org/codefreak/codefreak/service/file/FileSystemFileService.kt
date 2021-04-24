package org.codefreak.codefreak.service.file

import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["codefreak.files.adapter"], havingValue = "FILE_SYSTEM")
class FileSystemFileService : FileService {
  override fun readCollectionTar(collectionId: UUID): InputStream {
    TODO("Not yet implemented")
  }

  override fun writeCollectionTar(collectionId: UUID): OutputStream {
    TODO("Not yet implemented")
  }

  override fun collectionExists(collectionId: UUID): Boolean {
    TODO("Not yet implemented")
  }

  override fun deleteCollection(collectionId: UUID) {
    TODO("Not yet implemented")
  }

  override fun walkFileTree(collectionId: UUID): Sequence<FileMetaData> {
    TODO("Not yet implemented")
  }

  override fun listFiles(collectionId: UUID, path: String): Sequence<FileMetaData> {
    TODO("Not yet implemented")
  }

  override fun createFiles(collectionId: UUID, paths: Set<String>) {
    TODO("Not yet implemented")
  }

  override fun createDirectories(collectionId: UUID, paths: Set<String>) {
    TODO("Not yet implemented")
  }

  override fun containsFile(collectionId: UUID, path: String): Boolean {
    TODO("Not yet implemented")
  }

  override fun containsDirectory(collectionId: UUID, path: String): Boolean {
    TODO("Not yet implemented")
  }

  override fun deleteFiles(collectionId: UUID, paths: Set<String>) {
    TODO("Not yet implemented")
  }

  override fun moveFile(collectionId: UUID, sources: Set<String>, target: String) {
    TODO("Not yet implemented")
  }

  override fun writeFile(collectionId: UUID, path: String): OutputStream {
    TODO("Not yet implemented")
  }

  override fun readFile(collectionId: UUID, path: String): InputStream {
    TODO("Not yet implemented")
  }
}
