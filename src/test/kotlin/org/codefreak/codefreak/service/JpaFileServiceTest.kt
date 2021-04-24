package org.codefreak.codefreak.service

import java.util.UUID
import org.codefreak.codefreak.repository.FileCollectionRepository
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.service.file.JpaFileService
import org.mockito.InjectMocks
import org.mockito.Mock

class JpaFileServiceTest : FileServiceTest {
  override var collectionId = UUID(0, 0)

  @InjectMocks
  override var fileService: FileService = JpaFileService()

  @Mock
  override lateinit var fileCollectionRepository: FileCollectionRepository
}
