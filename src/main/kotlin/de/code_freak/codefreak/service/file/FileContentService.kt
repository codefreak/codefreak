package de.code_freak.codefreak.service.file

import de.code_freak.codefreak.service.BaseService
import de.code_freak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID

@Service
class FileContentService : BaseService() {

  @Autowired
  lateinit var fileService: FileService

  class VirtualFile(val path: String, val lastModifiedDate: Instant, val content: ByteArray)

  fun getFile(fileCollectionId: UUID, path: String): VirtualFile {
    val collection = fileService.readCollectionTar(fileCollectionId)
    collection.use {
      TarUtil.findFile(it, path) { entry, fileStream ->
        val output = ByteArrayOutputStream()
        StreamUtils.copy(fileStream, output)
        return VirtualFile(entry.name, entry.modTime.toInstant(), output.toByteArray())
      }
    }
  }
}