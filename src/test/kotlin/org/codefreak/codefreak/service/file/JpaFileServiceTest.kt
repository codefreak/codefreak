package org.codefreak.codefreak.service.file

import com.nhaarman.mockitokotlin2.any
import org.codefreak.codefreak.entity.FileCollection
import java.util.UUID
import org.codefreak.codefreak.repository.FileCollectionRepository
import org.junit.Before
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.Optional

class JpaFileServiceTest : FileServiceTest() {
  override var collectionId = UUID(0, 0)

  @InjectMocks
  override var fileService: FileService = JpaFileService()

  @Mock
  lateinit var fileCollectionRepository: FileCollectionRepository

  @Before
  fun init() {
    MockitoAnnotations.openMocks(this)

    val fileCollection = FileCollection(collectionId)
    Mockito.`when`(fileCollectionRepository.findById(any())).thenReturn(Optional.of(fileCollection))
  }
}
