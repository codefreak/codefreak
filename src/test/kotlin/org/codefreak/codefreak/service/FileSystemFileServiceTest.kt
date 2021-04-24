package org.codefreak.codefreak.service

import java.util.UUID
import org.codefreak.codefreak.repository.FileCollectionRepository
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.service.file.FileSystemFileService
import org.mockito.InjectMocks
import org.mockito.Mock

class FileSystemFileServiceTest : FileServiceTest {
  override var collectionId: UUID = UUID(0, 0)

  @InjectMocks
  override var fileService: FileService = FileSystemFileService()

  @Mock
  override lateinit var fileCollectionRepository: FileCollectionRepository
}
