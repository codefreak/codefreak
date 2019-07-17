package de.code_freak.codefreak.service.file

import de.code_freak.codefreak.entity.FileCollection
import de.code_freak.codefreak.repository.FileCollectionRepository
import de.code_freak.codefreak.service.EntityNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["code-freak.files.adapter"], havingValue = "JPA")
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

  override fun readCollectionTar(collectionId: UUID): InputStream {
    val collection = fileCollectionRepository.findById(collectionId)
        .orElseThrow { EntityNotFoundException("File not found") }
    return ByteArrayInputStream(collection.tar)
  }

  override fun collectionExists(collectionId: UUID): Boolean {
    return fileCollectionRepository.existsById(collectionId)
  }

  override fun deleteCollection(collectionId: UUID) {
    fileCollectionRepository.deleteById(collectionId)
  }
}
