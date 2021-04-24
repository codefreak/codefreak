package org.codefreak.codefreak.service.file

import java.util.UUID
import org.codefreak.codefreak.repository.FileCollectionRepository
import org.mockito.InjectMocks
import org.mockito.Mock

class FileSystemFileServiceTest : FileServiceTest {
  override var collectionId: UUID = UUID(0, 0)

  @InjectMocks
  override var fileService: FileService = FileSystemFileService()

  @Mock
  override lateinit var fileCollectionRepository: FileCollectionRepository
}
