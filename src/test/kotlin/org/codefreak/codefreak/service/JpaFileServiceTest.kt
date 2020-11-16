package org.codefreak.codefreak.service

import com.nhaarman.mockitokotlin2.any
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.codefreak.codefreak.entity.FileCollection
import org.codefreak.codefreak.repository.FileCollectionRepository
import org.codefreak.codefreak.service.file.JpaFileService
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.*

class JpaFileServiceTest {
  @Mock
  lateinit var fileCollectionRepository: FileCollectionRepository
  @InjectMocks
  val fileService = JpaFileService()

  @Before
  fun init() {
    MockitoAnnotations.initMocks(this)

    val fileCollection = FileCollection(UUID(0, 0))
    `when`(fileCollectionRepository.findById(any())).thenReturn(Optional.of(fileCollection))
  }

  @Test
  fun `createFile creates an empty file`() {
    val path = "file.txt"
    fileService.createFile(UUID(0, 0), path)

    TarArchiveInputStream(fileService.readCollectionTar(UUID(0, 0))).use {
      val entry = it.nextTarEntry
      assert(path == entry.name)
      assert(entry.isFile)
      assert(null == entry.file)
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createFile throws when the path already exists`() {
    fileService.createFile(UUID(0, 0), "file.txt")
    fileService.createFile(UUID(0, 0), "file.txt")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createFile throws on empty path name`() {
    fileService.createFile(UUID(0, 0), "")
  }
}
