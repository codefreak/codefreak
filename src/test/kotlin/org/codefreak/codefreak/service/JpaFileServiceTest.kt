package org.codefreak.codefreak.service

import com.nhaarman.mockitokotlin2.any
import java.util.Optional
import java.util.UUID
import org.codefreak.codefreak.entity.FileCollection
import org.codefreak.codefreak.repository.FileCollectionRepository
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.service.file.JpaFileService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class JpaFileServiceTest {
  private val collectionId = UUID(0, 0)

  @Mock
  lateinit var fileCollectionRepository: FileCollectionRepository
  @InjectMocks
  val fileService: FileService = JpaFileService()

  @Before
  fun init() {
    MockitoAnnotations.openMocks(this)

    val fileCollection = FileCollection(collectionId)
    `when`(fileCollectionRepository.findById(any())).thenReturn(Optional.of(fileCollection))
  }

  @Test
  fun `creates an empty file`() {
    fileService.createFiles(collectionId, setOf("file.txt"))
    assertTrue(fileService.containsFile(collectionId, "file.txt"))
  }

  @Test
  fun `creates multiple files`() {
    fileService.createFiles(collectionId, setOf("file1.txt", "file2.txt", "file3.txt"))
    assertTrue(fileService.containsFile(collectionId, "file1.txt"))
    assertTrue(fileService.containsFile(collectionId, "file2.txt"))
    assertTrue(fileService.containsFile(collectionId, "file3.txt"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `creating a file throws when the path already exists`() {
    fileService.createFiles(collectionId, setOf("file.txt"))
    fileService.createFiles(collectionId, setOf("file.txt")) // Throws because file already exists
  }

  @Test(expected = IllegalArgumentException::class)
  fun `creating a file two times at once throws`() {
    fileService.createFiles(collectionId, setOf("file.txt", "file.txt"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `creating a file throws on empty path name`() {
    fileService.createFiles(collectionId, setOf(""))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `creating a file throws when path is a directory`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    fileService.createFiles(collectionId, setOf("some/path"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `creating a file throws when the parent directory does not exist`() {
    fileService.createFiles(collectionId, setOf("parent/file.txt"))
  }

  @Test
  fun `creating a file keeps other files intact`() {
    fileService.createFiles(collectionId, setOf("other.txt"))
    fileService.createDirectories(collectionId, setOf("aDirectory"))
    fileService.createFiles(collectionId, setOf("file.txt"))

    assertTrue(fileService.containsFile(collectionId, "file.txt"))
    assertTrue(fileService.containsFile(collectionId, "other.txt"))
    assertTrue(fileService.containsDirectory(collectionId, "aDirectory"))
  }

  @Test
  fun `creating multiple files keeps other files intact`() {
    fileService.createFiles(collectionId, setOf("other.txt"))
    fileService.createDirectories(collectionId, setOf("aDirectory"))
    fileService.createFiles(collectionId, setOf("file1.txt", "file2.txt"))

    assertTrue(fileService.containsFile(collectionId, "file1.txt"))
    assertTrue(fileService.containsFile(collectionId, "file2.txt"))
    assertTrue(fileService.containsFile(collectionId, "other.txt"))
    assertTrue(fileService.containsDirectory(collectionId, "aDirectory"))
  }

  @Test
  fun `creates an empty directory`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    assertTrue(fileService.containsDirectory(collectionId, "some/path"))
  }

  @Test
  fun `creates multiple directories`() {
    fileService.createDirectories(collectionId, setOf("some/path", "some/other/path"))
    assertTrue(fileService.containsDirectory(collectionId, "some/path"))
    assertTrue(fileService.containsDirectory(collectionId, "some/other/path"))
  }

  @Test
  fun `creating a ignores silently when the directory already exists`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    fileService.createDirectories(collectionId, setOf("some/path"))
    assertTrue(fileService.containsDirectory(collectionId, "some/path"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `creating a directory throws on empty path name`() {
    fileService.createFiles(collectionId, setOf(""))
  }

  @Test
  fun `creating a directory keeps other files intact`() {
    fileService.createFiles(collectionId, setOf("other.txt"))
    fileService.createDirectories(collectionId, setOf("aDirectory"))
    fileService.createDirectories(collectionId, setOf("some/path"))

    assertTrue(fileService.containsFile(collectionId, "other.txt"))
    assertTrue(fileService.containsDirectory(collectionId, "aDirectory"))
    assertTrue(fileService.containsDirectory(collectionId, "some/path"))
  }

  @Test
  fun `creating multiple directories keeps other files intact`() {
    fileService.createFiles(collectionId, setOf("other.txt"))
    fileService.createDirectories(collectionId, setOf("aDirectory"))
    fileService.createDirectories(collectionId, setOf("some/path", "some/other/path"))

    assertTrue(fileService.containsFile(collectionId, "other.txt"))
    assertTrue(fileService.containsDirectory(collectionId, "aDirectory"))
    assertTrue(fileService.containsDirectory(collectionId, "some/path"))
    assertTrue(fileService.containsDirectory(collectionId, "some/other/path"))
  }

  @Test
  fun `finds an existing file`() {
    fileService.createFiles(collectionId, setOf("file.txt"))
    assertTrue(fileService.containsFile(collectionId, "file.txt"))
  }

  @Test
  fun `does not find not-existing files`() {
    assertFalse(fileService.containsFile(collectionId, "file.txt"))
  }

  @Test
  fun `does not find file if the path is a directory`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    assertFalse(fileService.containsFile(collectionId, "some/path"))
  }

  @Test
  fun `finds an existing directory`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    assertTrue(fileService.containsDirectory(collectionId, "some/path"))
  }

  @Test
  fun `does not find not-existing directories`() {
    assertFalse(fileService.containsDirectory(collectionId, "some/path"))
  }

  @Test
  fun `does not find directory if the path is a file`() {
    fileService.createFiles(collectionId, setOf("file.txt"))
    assertFalse(fileService.containsDirectory(collectionId, "file.txt"))
  }

  @Test
  fun `deletes an existing file`() {
    fileService.createFiles(collectionId, setOf("file.txt"))

    fileService.deleteFiles(collectionId, setOf("file.txt"))

    assertFalse(fileService.containsFile(collectionId, "file.txt"))
  }

  @Test
  fun `deletes multiple existing files`() {
    fileService.createFiles(collectionId, setOf("file1.txt", "file2.txt"))

    fileService.deleteFiles(collectionId, setOf("file1.txt", "file2.txt"))

    assertFalse(fileService.containsFile(collectionId, "file1.txt"))
    assertFalse(fileService.containsFile(collectionId, "file2.txt"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `deleting a file throws when path does not exist`() {
    fileService.deleteFiles(collectionId, setOf("file.txt"))
  }

  @Test
  fun `deleting multiple files does not delete any file when one of the files does not exist`() {
    fileService.createFiles(collectionId, setOf("file1.txt"))

    try {
      fileService.deleteFiles(collectionId, setOf("file1.txt", "file2.txt"))
      fail() // An IllegalArgumentException should be thrown
    } catch (e: IllegalArgumentException) {}

    assertTrue(fileService.containsFile(collectionId, "file1.txt"))
  }

  @Test
  fun `keeps other files and directories intact when deleting a file`() {
    fileService.createFiles(collectionId, setOf("file.txt"))
    fileService.createFiles(collectionId, setOf("DO_NOT_DELETE.txt"))
    fileService.createDirectories(collectionId, setOf("some/path"))

    fileService.deleteFiles(collectionId, setOf("file.txt"))

    assertFalse(fileService.containsFile(collectionId, "file.txt"))
    assertTrue(fileService.containsFile(collectionId, "DO_NOT_DELETE.txt"))
    assertTrue(fileService.containsDirectory(collectionId, "some/path"))
  }

  @Test
  fun `deletes existing directory`() {
    fileService.createDirectories(collectionId, setOf("some/path"))

    fileService.deleteFiles(collectionId, setOf("some/path"))

    assertFalse(fileService.containsDirectory(collectionId, "some/path"))
  }

  @Test
  fun `deletes multiple existing directories`() {
    fileService.createDirectories(collectionId, setOf("some/path", "some/other/path"))

    fileService.deleteFiles(collectionId, setOf("some/path", "some/other/path"))

    assertFalse(fileService.containsDirectory(collectionId, "some/path"))
    assertFalse(fileService.containsDirectory(collectionId, "some/other/path"))
  }

  @Test
  fun `deletes existing files and directories`() {
    fileService.createDirectories(collectionId, setOf("some/path", "some/other/path", "file1.txt", "file2.txt"))

    fileService.deleteFiles(collectionId, setOf("some/path", "some/other/path", "file1.txt", "file2.txt"))

    assertFalse(fileService.containsDirectory(collectionId, "some/path"))
    assertFalse(fileService.containsDirectory(collectionId, "some/other/path"))
    assertFalse(fileService.containsFile(collectionId, "file1.txt"))
    assertFalse(fileService.containsFile(collectionId, "file2.txt"))
  }

  @Test
  fun `keeps other files and directories intact when deleting a directory`() {
    fileService.createFiles(collectionId, setOf("file.txt"))
    fileService.createDirectories(collectionId, setOf("DO_NOT_DELETE"))
    fileService.createDirectories(collectionId, setOf("some/path"))

    fileService.deleteFiles(collectionId, setOf("some/path"))

    assertTrue(fileService.containsFile(collectionId, "file.txt"))
    assertTrue(fileService.containsDirectory(collectionId, "DO_NOT_DELETE"))
    assertFalse(fileService.containsDirectory(collectionId, "some/path"))
  }

  @Test
  fun `deletes directory content recursively`() {
    val directoryToDelete = "some/path"
    val fileToRecursivelyDelete = "some/path/file.txt"
    val directoryToRecursivelyDelete = "some/path/some/path"
    val fileToBeUnaffected = "file.txt"

    fileService.createDirectories(collectionId, setOf(directoryToDelete))
    fileService.createFiles(collectionId, setOf(fileToRecursivelyDelete))
    fileService.createDirectories(collectionId, setOf(directoryToRecursivelyDelete))
    fileService.createFiles(collectionId, setOf(fileToBeUnaffected))

    fileService.deleteFiles(collectionId, setOf(directoryToDelete))

    assertFalse(fileService.containsDirectory(collectionId, directoryToDelete))
    assertFalse(fileService.containsFile(collectionId, fileToRecursivelyDelete))
    assertFalse(fileService.containsDirectory(collectionId, directoryToRecursivelyDelete))
    assertTrue(fileService.containsFile(collectionId, fileToBeUnaffected))
  }

  @Test
  fun `writes file contents correctly`() {
    val contents = byteArrayOf(42)
    fileService.createFiles(collectionId, setOf("file.txt"))

    fileService.writeFile(collectionId, "file.txt").use {
      it.write(contents)
    }

    assertTrue(fileService.containsFile(collectionId, "file.txt"))
    assertTrue(equals(fileService.readFile(collectionId, "file.txt").readBytes(), contents))
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
  fun `writing file contents throws for directories`() {
    fileService.createDirectories(collectionId, setOf("some/path"))

    fileService.writeFile(collectionId, "some/path").use {
      it.write(byteArrayOf(42))
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `writing file contents throws if path does not exist`() {
    fileService.writeFile(collectionId, "file.txt").use {
      it.write(byteArrayOf(42))
    }
  }

  @Test
  fun `moves existing file`() {
    fileService.createFiles(collectionId, setOf("file.txt"))

    fileService.moveFile(collectionId, setOf("file.txt"), "new.txt")

    assertFalse(fileService.containsFile(collectionId, "file.txt"))
    assertTrue(fileService.containsFile(collectionId, "new.txt"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `moving a file throws when source path does not exist`() {
    fileService.moveFile(collectionId, setOf("file.txt"), "new.txt")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `moving a file throws when target file path already exists`() {
    fileService.createFiles(collectionId, setOf("file.txt"))
    fileService.createFiles(collectionId, setOf("new.txt"))

    fileService.moveFile(collectionId, setOf("file.txt"), "new.txt")
  }

  @Test
  fun `moving a file does not change file contents`() {
    val contents = byteArrayOf(42)
    fileService.createFiles(collectionId, setOf("file.txt"))
    fileService.writeFile(collectionId, "file.txt").use {
      it.write(contents)
    }

    fileService.moveFile(collectionId, setOf("file.txt"), "new.txt")

    assertTrue(equals(contents, fileService.readFile(collectionId, "new.txt").readBytes()))
  }

  @Test
  fun `moves existing directory`() {
    fileService.createDirectories(collectionId, setOf("some/path"))

    fileService.moveFile(collectionId, setOf("some/path"), "new")

    assertFalse(fileService.containsDirectory(collectionId, "some/path"))
    assertTrue(fileService.containsDirectory(collectionId, "new"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `moving a directory throws when source directory is moved to itself`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    fileService.createDirectories(collectionId, setOf("some/path/inner"))

    fileService.moveFile(collectionId, setOf("some/path"), "some/path/inner")
  }

  @Test
  fun `moving a directory moves inner hierarchy correctly`() {
    val innerDirectory = "some/path/inner"
    val innerFile1 = "some/path/inner/file.txt"
    val innerFile1Contents = byteArrayOf(42)
    val innerFile2 = "some/path/file.txt"
    val innerFile2Contents = byteArrayOf(17)

    fileService.createDirectories(collectionId, setOf("some/path"))
    fileService.createDirectories(collectionId, setOf(innerDirectory))
    fileService.createFiles(collectionId, setOf(innerFile1))
    fileService.writeFile(collectionId, innerFile1).use {
      it.write(innerFile1Contents)
    }
    fileService.createFiles(collectionId, setOf(innerFile2))
    fileService.writeFile(collectionId, innerFile2).use {
      it.write(innerFile2Contents)
    }

    fileService.moveFile(collectionId, setOf("some/path"), "new")

    assertFalse(fileService.containsDirectory(collectionId, "some/path"))
    assertFalse(fileService.containsDirectory(collectionId, innerDirectory))
    assertFalse(fileService.containsFile(collectionId, innerFile1))
    assertFalse(fileService.containsFile(collectionId, innerFile2))
    assertTrue(fileService.containsDirectory(collectionId, "new"))
    assertTrue(fileService.containsDirectory(collectionId, "new/inner"))
    assertTrue(fileService.containsFile(collectionId, "new/inner/file.txt"))
    assertTrue(equals(fileService.readFile(collectionId, "new/inner/file.txt").readBytes(), innerFile1Contents))
    assertTrue(fileService.containsFile(collectionId, "new/file.txt"))
    assertTrue(equals(fileService.readFile(collectionId, "new/file.txt").readBytes(), innerFile2Contents))
  }
}
