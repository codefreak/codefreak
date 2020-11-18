package org.codefreak.codefreak.service

import com.nhaarman.mockitokotlin2.any
import java.util.Optional
import java.util.UUID
import org.codefreak.codefreak.entity.FileCollection
import org.codefreak.codefreak.repository.FileCollectionRepository
import org.codefreak.codefreak.service.file.JpaFileService
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class JpaFileServiceTest {
  private val collectionId = UUID(0, 0)
  private val filePath = "file.txt"
  private val directoryPath = "some/path"

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
    createFile(filePath)
    assert(containsFile(filePath))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createFile throws when the path already exists`() {
    createFile(filePath)
    createFile(filePath) // Throws because file already exists
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createFile throws on empty path name`() {
    createFile("")
  }

  @Test
  fun `createFile keeps other files intact`() {
    createFile("other.txt")
    createDirectory("aDirectory")
    createFile(filePath)

    assert(fileService.containsFile(collectionId, filePath))
    assert(fileService.containsFile(collectionId, "other.txt"))
    assert(fileService.containsDirectory(collectionId, "aDirectory"))
  }

  @Test
  fun `createDirectory creates an empty directory`() {
    createDirectory(directoryPath)
    assert(containsDirectory(directoryPath))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createDirectory throws when the path already exists`() {
    createDirectory(directoryPath)
    createDirectory(directoryPath) // Throws because directory already exists
  }

  @Test(expected = IllegalArgumentException::class)
  fun `createDirectory throws on empty path name`() {
    createFile("")
  }

  @Test
  fun `createDirectory keeps other files intact`() {
    createFile("other.txt")
    createDirectory("aDirectory")
    createDirectory(directoryPath)

    assert(containsFile("other.txt"))
    assert(containsDirectory("aDirectory"))
    assert(containsDirectory(directoryPath))
  }

  @Test
  fun `deleteFile deletes existing file`() {
    createFile(filePath)

    deleteFile(filePath)

    assert(!containsFile(filePath))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `deleteFile throws when path does not exist`() {
    deleteFile(filePath)
  }

  @Test
  fun `deleteFile keeps other files and directories intact`() {
    createFile(filePath)
    createFile("DO_NOT_DELETE.txt")
    createDirectory(directoryPath)

    deleteFile(filePath)

    assert(!containsFile(filePath))
    assert(containsFile("DO_NOT_DELETE.txt"))
    assert(containsDirectory(directoryPath))
  }

  @Test
  fun `deleteDirectory deletes existing directory`() {
    createDirectory(directoryPath)

    deleteDirectory(directoryPath)

    assert(!containsDirectory(directoryPath))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `deleteDirectory throws when path does not exist`() {
    deleteDirectory(directoryPath)
  }

  @Test
  fun `deleteDirectory keeps other files and directories intact`() {
    createFile(filePath)
    createDirectory("DO_NOT_DELETE")
    createDirectory(directoryPath)

    deleteDirectory(directoryPath)

    assert(containsFile(filePath))
    assert(containsDirectory("DO_NOT_DELETE"))
    assert(!containsDirectory(directoryPath))
  }

  @Test
  fun `deleteDirectory deletes directory content recursively`() {
    val directoryToDelete = directoryPath
    val fileToRecursivelyDelete = "$directoryPath/$filePath"
    val directoryToRecursivelyDelete = "$directoryPath/$directoryPath"
    val fileToBeUnaffected = filePath

    createDirectory(directoryToDelete)
    createFile(fileToRecursivelyDelete)
    createDirectory(directoryToRecursivelyDelete)
    createFile(fileToBeUnaffected)

    deleteDirectory(directoryToDelete)

    assert(!containsDirectory(directoryToDelete))
    assert(!containsFile(fileToRecursivelyDelete))
    assert(!containsDirectory(directoryToRecursivelyDelete))
    assert(containsFile(fileToBeUnaffected))
  }

  @Test
  fun `filePutContents puts the file contents correctly`() {
    val contents = byteArrayOf(42)
    createFile(filePath)

    filePutContents(filePath, contents)

    assert(containsFile(filePath))
    assert(equals(getFileContents(filePath), contents))
  }

  private fun equals(a: ByteArray, b: ByteArray): Boolean {
    if (a.size != b.size) {
      return false
    }

    a.forEachIndexed { index, byte ->
      if (byte != b[index]) {
        return false
      }
    }

    return true
  }

  @Test(expected = IllegalArgumentException::class)
  fun `filePutContents throws for directories`() {
    createDirectory(directoryPath)

    filePutContents(directoryPath, byteArrayOf(42))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `filePutContents throws if path does not exist`() {
    filePutContents(filePath, byteArrayOf(42))
  }

  @Test
  fun `moveFile moves existing file`() {
    createFile(filePath)

    moveFile(filePath, "new.txt")

    assert(!containsFile(filePath))
    assert(containsFile("new.txt"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `moveFile throws when source path does not exist`() {
    moveFile(filePath, "new.txt")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `moveFile throws when target path already exists`() {
    createFile(filePath)
    createFile("new.txt")

    moveFile(filePath, "new.txt")
  }

  @Test
  fun `moveFile does not change file contents`() {
    createFile(filePath)
    filePutContents(filePath, byteArrayOf(42))

    moveFile(filePath, "new.txt")

    assert(equals(byteArrayOf(42), getFileContents("new.txt")))
  }

  @Test
  fun `moveDirectory moves existing directory`() {
    createDirectory(directoryPath)

    moveDirectory(directoryPath, "new")

    assert(!containsDirectory(directoryPath))
    assert(containsDirectory("new"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `moveDirectory throws when source path does not exist`() {
    moveDirectory(directoryPath, "new")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `moveDirectory throws when target path already exists`() {
    createDirectory(directoryPath)
    createDirectory("new")

    moveDirectory(directoryPath, "new")
  }

  @Test
  fun `moveDirectory moves inner hierarchy correctly`() {
    val innerDirectory = "$directoryPath/inner"
    val innerFile1 = "$innerDirectory/$filePath"
    val innerFile1Contents = byteArrayOf(42)
    val innerFile2 = "$directoryPath/$filePath"
    val innerFile2Contents = byteArrayOf(17)

    createDirectory(directoryPath)
    createDirectory(innerDirectory)
    createFile(innerFile1)
    filePutContents(innerFile1, innerFile1Contents)
    createFile(innerFile2)
    filePutContents(innerFile2, innerFile2Contents)

    moveDirectory(directoryPath, "new")

    assert(!containsDirectory(directoryPath))
    assert(!containsDirectory(innerDirectory))
    assert(!containsFile(innerFile1))
    assert(!containsFile(innerFile2))
    assert(containsDirectory("new"))
    assert(containsDirectory("new/inner"))
    assert(containsFile("new/inner/$filePath"))
    assert(equals(getFileContents("new/inner/$filePath"), innerFile1Contents))
    assert(containsFile("new/$filePath"))
    assert(equals(getFileContents("new/$filePath"), innerFile2Contents))
  }

  private fun createFile(path: String) = fileService.createFile(collectionId, path)

  private fun createDirectory(path: String) = fileService.createDirectory(collectionId, path)

  private fun deleteFile(path: String) = fileService.deleteFile(collectionId, path)

  private fun deleteDirectory(path: String) = fileService.deleteDirectory(collectionId, path)

  private fun containsFile(path: String): Boolean = fileService.containsFile(collectionId, path)

  private fun containsDirectory(path: String): Boolean = fileService.containsDirectory(collectionId, path)

  private fun filePutContents(path: String, contents: ByteArray) = fileService.filePutContents(collectionId, path, contents)

  private fun getFileContents(path: String): ByteArray = fileService.getFileContents(collectionId, path)

  private fun moveFile(from: String, to: String) = fileService.moveFile(collectionId, from, to)

  private fun moveDirectory(from: String, to: String) = fileService.moveDirectory(collectionId, from, to)
}
