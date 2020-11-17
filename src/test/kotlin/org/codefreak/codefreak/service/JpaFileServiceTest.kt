package org.codefreak.codefreak.service

import com.nhaarman.mockitokotlin2.any
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
  private val collectionId = UUID(0, 0)
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

    val fileCollection = FileCollection(collectionId)
    `when`(fileCollectionRepository.findById(any())).thenReturn(Optional.of(fileCollection))
  }

  @Test
  fun `createFile creates an empty file`() {
    fileService.createFile(collectionId, filePath)
    assert(fileService.containsFile(collectionId, filePath))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createFile throws when the path already exists`() {
    fileService.createFile(collectionId, filePath)
    fileService.createFile(collectionId, filePath)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createFile throws on empty path name`() {
    fileService.createFile(collectionId, emptyPath)
  }

  @Test
  fun `createFile keeps other files intact`() {
    val otherFile = "other.txt"
    val otherDirectory = "aDirectory"
    fileService.createFile(collectionId, otherFile)
    fileService.createDirectory(collectionId, otherDirectory)
    fileService.createFile(collectionId, filePath)

    assert(fileService.containsFile(collectionId, filePath))
    assert(fileService.containsFile(collectionId, otherFile))
    assert(fileService.containsDirectory(collectionId, otherDirectory))
  }

  @Test
  fun `createDirectory creates an empty directory`() {
    fileService.createDirectory(collectionId, directoryPath)

    assert(fileService.containsDirectory(collectionId, directoryPath))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createDirectory throws when the path already exists`() {
    fileService.createFile(collectionId, directoryPath)
    fileService.createFile(collectionId, directoryPath)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createDirectory throws on empty path name`() {
    fileService.createFile(collectionId, emptyPath)
  }

  @Test
  fun `createDirectory keeps other files intact`() {
    val otherFile = "other.txt"
    val otherDirectory = "aDirectory"
    fileService.createFile(collectionId, otherFile)
    fileService.createDirectory(collectionId, otherDirectory)
    fileService.createDirectory(collectionId, directoryPath)

    assert(fileService.containsDirectory(collectionId, directoryPath))
    assert(fileService.containsFile(collectionId, otherFile))
    assert(fileService.containsDirectory(collectionId, otherDirectory))
  }

  @Test
  fun `deleteFile deletes existing file`() {
    fileService.createFile(collectionId, filePath)
    fileService.deleteFile(collectionId, filePath)
    assert(!fileService.containsFile(collectionId, filePath))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `deleteFile throws when path does not exist`() {
    fileService.deleteFile(collectionId, filePath)
  }

  @Test
  fun `deleteFile keeps other files and directories intact`() {
    val intactFile = "DO_NOT_DELETE.txt"
    fileService.createFile(collectionId, filePath)
    fileService.createFile(collectionId, intactFile)
    fileService.createDirectory(collectionId, directoryPath)

    fileService.deleteFile(collectionId, filePath)

    assert(!fileService.containsFile(collectionId, filePath))
    assert(fileService.containsFile(collectionId, intactFile))
    assert(fileService.containsDirectory(collectionId, directoryPath))
  }

  @Test
  fun `deleteDirectory deletes existing directory`() {
    fileService.createDirectory(collectionId, directoryPath)
    fileService.deleteDirectory(collectionId, directoryPath)
    assert(!fileService.containsDirectory(collectionId, directoryPath))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `deleteDirectory throws when path does not exist`() {
    fileService.deleteDirectory(collectionId, directoryPath)
  }

  @Test
  fun `deleteDirectory keeps other files and directories intact`() {
    val intactDirectory = "DO_NOT_DELETE"
    fileService.createFile(collectionId, filePath)
    fileService.createDirectory(collectionId, intactDirectory)
    fileService.createDirectory(collectionId, directoryPath)

    fileService.deleteDirectory(collectionId, directoryPath)

    assert(fileService.containsFile(collectionId, filePath))
    assert(fileService.containsDirectory(collectionId, intactDirectory))
    assert(!fileService.containsDirectory(collectionId, directoryPath))
  }

  @Test
  fun `deleteDirectory deletes directory content recursively`() {
    val directoryToDelete = directoryPath
    val fileToRecursivelyDelete = "$directoryPath/$filePath"
    val directoryToRecursivelyDelete = "$directoryPath/$directoryPath"
    val fileToBeUnaffected = filePath

    fileService.createDirectory(collectionId, directoryToDelete)
    fileService.createFile(collectionId, fileToRecursivelyDelete)
    fileService.createDirectory(collectionId, directoryToRecursivelyDelete)
    fileService.createFile(collectionId, fileToBeUnaffected)

    fileService.deleteDirectory(collectionId, directoryToDelete)

    assert(!fileService.containsDirectory(collectionId, directoryToDelete))
    assert(!fileService.containsFile(collectionId, fileToRecursivelyDelete))
    assert(!fileService.containsDirectory(collectionId, directoryToRecursivelyDelete))
    assert(fileService.containsFile(collectionId, fileToBeUnaffected))
  }
}
