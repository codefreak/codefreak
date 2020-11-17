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
  private val id = UUID(0, 0)
  private val filePath = "file.txt"
  private val directoryPath = "some/path/"
  private val emptyPath = ""

  @Mock
  lateinit var fileCollectionRepository: FileCollectionRepository
  @InjectMocks
  val fileService = JpaFileService()

  @Before
  fun init() {
    MockitoAnnotations.initMocks(this)

    val fileCollection = FileCollection(id)
    `when`(fileCollectionRepository.findById(any())).thenReturn(Optional.of(fileCollection))
  }

  @Test
  fun `createFile creates an empty file`() {
    fileService.createFile(id, filePath)

    TarArchiveInputStream(fileService.readCollectionTar(id)).use {
      val entry = it.nextTarEntry
      assert(filePath == entry.name)
      assert(entry.isFile)
      assert(null == entry.file)
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createFile throws when the path already exists`() {
    fileService.createFile(id, filePath)
    fileService.createFile(id, filePath)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createFile throws on empty path name`() {
    fileService.createFile(id, emptyPath)
  }

  @Test
  fun `createDirectory creates an empty directory`() {
    fileService.createDirectory(id, directoryPath)

    TarArchiveInputStream(fileService.readCollectionTar(id)).use {
      val entry = it.nextTarEntry
      assert(directoryPath == entry.name)
      assert(entry.isDirectory)
      assert(null == entry.file)
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createDirectory throws when the path already exists`() {
    fileService.createFile(id, directoryPath)
    fileService.createFile(id, directoryPath)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createDirectory throws on empty path name`() {
    fileService.createFile(id, emptyPath)
  }
}
