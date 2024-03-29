package org.codefreak.codefreak.service.file

import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

abstract class FileServiceTest {
  abstract var collectionId: UUID
  abstract var fileService: FileService

  @Test
  fun `lists all existing files and directories`() {
    fileService.createDirectories(collectionId, setOf("some/path", "some/other/path"))
    fileService.createFiles(
      collectionId,
      setOf("file1.txt", "file2.txt", "some/file3.txt", "some/path/file4.txt", "some/other/path/file5.txt")
    )

    assertNotNull(fileService.walkFileTree(collectionId).find { it.path == "/some" })
    assertNotNull(fileService.walkFileTree(collectionId).find { it.path == "/some/path" })
    assertNotNull(fileService.walkFileTree(collectionId).find { it.path == "/some/other" })
    assertNotNull(fileService.walkFileTree(collectionId).find { it.path == "/some/other/path" })
    assertNotNull(fileService.walkFileTree(collectionId).find { it.path == "/file1.txt" })
    assertNotNull(fileService.walkFileTree(collectionId).find { it.path == "/file2.txt" })
    assertNotNull(fileService.walkFileTree(collectionId).find { it.path == "/some/file3.txt" })
    assertNotNull(fileService.walkFileTree(collectionId).find { it.path == "/some/path/file4.txt" })
    assertNotNull(fileService.walkFileTree(collectionId).find { it.path == "/some/other/path/file5.txt" })
  }

  @Test
  fun `root dir does always exist with no files`() {
    assertTrue(fileService.listFiles(collectionId, "/").use { it.count() } == 0L)
  }

  @Test
  fun `lists all files and directories in a path`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    fileService.createFiles(collectionId, setOf("file1.txt", "file2.txt"))

    assertNotNull(fileService.listFiles(collectionId, "/").find { it.path == "/some" })
    assertNotNull(fileService.listFiles(collectionId, "/").find { it.path == "/file1.txt" })
    assertNotNull(fileService.listFiles(collectionId, "/").find { it.path == "/file2.txt" })
  }

  @Test
  fun `listing all files and directories in a path lists no other files and directories at a deeper level`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    fileService.createFiles(collectionId, setOf("file1.txt", "file2.txt", "some/file3.txt"))

    assertNotNull(fileService.listFiles(collectionId, "/").find { it.path == "/some" })
    assertNull(fileService.listFiles(collectionId, "/").find { it.path == "/some/path" })
    assertNotNull(fileService.listFiles(collectionId, "/").find { it.path == "/file1.txt" })
    assertNotNull(fileService.listFiles(collectionId, "/").find { it.path == "/file2.txt" })
    assertNull(fileService.listFiles(collectionId, "/").find { it.path == "/some/file3.txt" })
  }

  @Test
  fun `listing all files and directories in a path lists no other files and directories at a higher level`() {
    fileService.createDirectories(collectionId, setOf("some/path", "other"))
    fileService.createFiles(collectionId, setOf("file1.txt", "some/file2.txt"))

    assertNull(fileService.listFiles(collectionId, "/some").find { it.path == "/other" })
    assertNull(fileService.listFiles(collectionId, "/some").find { it.path == "/file1.txt" })
    assertNotNull(fileService.listFiles(collectionId, "/some").find { it.path == "/some/path" })
    assertNotNull(fileService.listFiles(collectionId, "/some").find { it.path == "/some/file2.txt" })
  }

  @Test
  fun `listing all files and directories in a path throws when the path does not exist`() {
    assertThrows(IllegalArgumentException::class.java) {
      fileService.listFiles(collectionId, "/some/path").close()
    }
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

  @Test
  fun `creating a file does not throw when the path already exists`() {
    fileService.createFiles(collectionId, setOf("file.txt"))
    fileService.createFiles(collectionId, setOf("file.txt"))
  }

  @Test
  fun `creating a file throws on empty path name`() {
    assertThrows(IllegalArgumentException::class.java) {
      fileService.createFiles(collectionId, setOf(""))
    }
  }

  @Test
  fun `creating a file throws when path is a directory`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    assertThrows(IllegalArgumentException::class.java) {
      fileService.createFiles(collectionId, setOf("some/path"))
    }
  }

  @Test
  fun `creating a file doesn't throw when the parent directory does not exist`() {
    fileService.createFiles(collectionId, setOf("parent/file.txt"))
    assertTrue(fileService.containsFile(collectionId, "parent/file.txt"))
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
  fun `creates a directory creates parent directories`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    assertTrue(fileService.containsDirectory(collectionId, "some"))
    assertTrue(fileService.containsDirectory(collectionId, "some/path"))
  }

  @Test
  fun `creates multiple directories`() {
    fileService.createDirectories(collectionId, setOf("some/path", "some/other/path"))
    assertTrue(fileService.containsDirectory(collectionId, "some/path"))
    assertTrue(fileService.containsDirectory(collectionId, "some/other/path"))
  }

  @Test
  fun `creating a directory ignores silently when the directory already exists`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    fileService.createDirectories(collectionId, setOf("some/path"))
    assertTrue(fileService.containsDirectory(collectionId, "some/path"))
  }

  @Test
  fun `creating a directory throws on empty path name`() {
    assertThrows(IllegalArgumentException::class.java) {
      fileService.createFiles(collectionId, setOf(""))
    }
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

  @Test
  fun `deleting a file throws when path does not exist`() {
    assertThrows(IllegalArgumentException::class.java) {
      fileService.deleteFiles(collectionId, setOf("file.txt"))
    }
  }

  @Test
  fun `deleting multiple files does not delete any file when one of the files does not exist`() {
    fileService.createFiles(collectionId, setOf("file1.txt"))

    assertThrows(IllegalArgumentException::class.java) {
      fileService.deleteFiles(collectionId, setOf("file1.txt", "file2.txt"))
    }

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
  fun `writes and reads file contents correctly`() {
    val contents = byteArrayOf(42)
    fileService.createFiles(collectionId, setOf("file.txt"))

    fileService.writeFile(collectionId, "file.txt").use {
      it.write(contents)
    }

    assertTrue(fileService.containsFile(collectionId, "file.txt"))
    assertTrue(equals(fileService.readFile(collectionId, "file.txt").use { it.readBytes() }, contents))
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

  @Test
  fun `writing file contents throws for directories`() {
    fileService.createDirectories(collectionId, setOf("some/path"))

    assertThrows(IllegalArgumentException::class.java) {
      fileService.writeFile(collectionId, "some/path").use {
        it.write(byteArrayOf(42))
      }
    }
  }

  @Test
  fun `writing file contents throws when path is a directory`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    assertThrows(IllegalArgumentException::class.java) {
      fileService.writeFile(collectionId, "some/path").use {
        it.write(byteArrayOf(42))
      }
    }
  }

  @Test
  fun `writing file contents when path does not exist creates the file and writes the content`() {
    fileService.writeFile(collectionId, "file.txt").use {
      it.write(byteArrayOf(42))
    }

    assertTrue(fileService.containsFile(collectionId, "file.txt"))
    assertTrue(equals(fileService.readFile(collectionId, "file.txt").use { it.readBytes() }, byteArrayOf(42)))
  }

  @Test
  fun `writing file contents overrides existing file contents`() {
    fileService.createFiles(collectionId, setOf("file.txt"))
    val oldContent = byteArrayOf(42)
    val newContent = byteArrayOf(17)

    fileService.writeFile(collectionId, "file.txt").use {
      it.write(oldContent)
    }

    fileService.writeFile(collectionId, "file.txt").use {
      it.write(newContent)
    }

    assertFalse(equals(fileService.readFile(collectionId, "file.txt").use { it.readBytes() }, oldContent))
    assertTrue(equals(fileService.readFile(collectionId, "file.txt").use { it.readBytes() }, newContent))
  }

  @Test
  fun `reads an existing empty file`() {
    fileService.createFiles(collectionId, setOf("file.txt"))
    assertTrue(equals(fileService.readFile(collectionId, "file.txt").use { it.readBytes() }, byteArrayOf()))
  }

  @Test
  fun `reading file contents throws for directories`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    assertThrows(IllegalArgumentException::class.java) {
      fileService.readFile(collectionId, "some/path").close()
    }
  }

  @Test
  fun `reading file contents throws if path does not exist`() {
    assertThrows(IllegalArgumentException::class.java) {
      fileService.readFile(collectionId, "file.txt").close()
    }
  }

  @Test
  fun `rename existing file`() {
    fileService.createFiles(collectionId, setOf("file.txt"))

    fileService.renameFile(collectionId, "file.txt", "new.txt")

    assertFalse(fileService.containsFile(collectionId, "file.txt"))
    assertTrue(fileService.containsFile(collectionId, "new.txt"))
  }

  @Test
  fun `moves existing files`() {
    fileService.createDirectories(collectionId, setOf("some/path", "some/path/other"))
    fileService.createFiles(collectionId, setOf("file1.txt", "file2.txt", "some/path/other/file3.txt"))

    fileService.moveFile(collectionId, setOf("file1.txt", "file2.txt", "some/path/other/file3.txt"), "some/path")

    assertFalse(fileService.containsFile(collectionId, "file1.txt"))
    assertFalse(fileService.containsFile(collectionId, "file2.txt"))
    assertFalse(fileService.containsFile(collectionId, "some/path/other/file3.txt"))
    assertTrue(fileService.containsFile(collectionId, "some/path/file1.txt"))
    assertTrue(fileService.containsFile(collectionId, "some/path/file2.txt"))
    assertTrue(fileService.containsFile(collectionId, "some/path/file3.txt"))
  }

  @Test
  fun `moving a file throws when source path does not exist`() {
    assertThrows(IllegalArgumentException::class.java) {
      fileService.moveFile(collectionId, setOf("file.txt"), "new.txt")
    }
  }

  @Test
  fun `moving files does not move any files when a file in the source path does not exist`() {
    fileService.createFiles(collectionId, setOf("file1.txt"))
    fileService.createDirectories(collectionId, setOf("new"))

    assertThrows(IllegalArgumentException::class.java) {
      fileService.moveFile(collectionId, setOf("file1.txt", "file2.txt"), "new")
    }

    assertTrue(fileService.containsFile(collectionId, "file1.txt"))
    assertFalse(fileService.containsFile(collectionId, "new/file1.txt"))
  }

  @Test
  fun `moving a file throws when target file path already exists`() {
    fileService.createFiles(collectionId, setOf("file.txt"))
    fileService.createFiles(collectionId, setOf("new.txt"))

    assertThrows(IllegalArgumentException::class.java) {
      fileService.moveFile(collectionId, setOf("file.txt"), "new.txt")
    }
  }

  @Test
  fun `moving multiple files throws when target file path does not exist`() {
    fileService.createFiles(collectionId, setOf("file1.txt", "file2.txt"))

    assertThrows(IllegalArgumentException::class.java) {
      fileService.moveFile(collectionId, setOf("file1.txt", "file2.txt"), "new")
    }
  }

  @Test
  fun `renaming a file does not change file contents`() {
    val contents = byteArrayOf(42)
    fileService.createFiles(collectionId, setOf("file.txt"))
    fileService.writeFile(collectionId, "file.txt").use {
      it.write(contents)
    }

    fileService.renameFile(collectionId, "file.txt", "new.txt")

    assertTrue(equals(contents, fileService.readFile(collectionId, "new.txt").use { it.readBytes() }))
  }

  @Test
  fun `rename existing directory`() {
    fileService.createDirectories(collectionId, setOf("some/path"))

    fileService.renameFile(collectionId, "some/path", "new")

    assertFalse(fileService.containsDirectory(collectionId, "some/path"))
    assertTrue(fileService.containsDirectory(collectionId, "new"))
  }

  @Test
  fun `moves existing directories`() {
    fileService.createDirectories(collectionId, setOf("some/path", "some/other", "new"))

    fileService.moveFile(collectionId, setOf("some/path", "some/other"), "new")

    assertFalse(fileService.containsDirectory(collectionId, "some/path"))
    assertFalse(fileService.containsDirectory(collectionId, "some/other"))
    assertTrue(fileService.containsDirectory(collectionId, "new/path"))
    assertTrue(fileService.containsDirectory(collectionId, "new/other"))
  }

  @Test
  fun `moving a directory throws when source directory is moved to itself`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    fileService.createDirectories(collectionId, setOf("some/path/inner"))

    assertThrows(IllegalArgumentException::class.java) {
      fileService.moveFile(collectionId, setOf("some/path"), "some/path/inner")
    }
  }

  @Test
  fun `renaming a directory moves inner hierarchy correctly`() {
    val innerFile2Contents = byteArrayOf(17)

    fileService.createDirectories(collectionId, setOf("some/path"))
    fileService.createDirectories(collectionId, setOf("some/path/inner"))
    fileService.createFiles(collectionId, setOf("some/path/inner/file.txt"))
    fileService.writeFile(collectionId, "some/path/inner/file.txt").use {
      it.write(byteArrayOf(42))
    }
    fileService.createFiles(collectionId, setOf("some/path/file.txt"))
    fileService.writeFile(collectionId, "some/path/file.txt").use {
      it.write(innerFile2Contents)
    }

    fileService.renameFile(collectionId, "some/path", "new")

    assertFalse(fileService.containsDirectory(collectionId, "some/path"))
    assertFalse(fileService.containsDirectory(collectionId, "some/path/inner"))
    assertFalse(fileService.containsFile(collectionId, "some/path/inner/file.txt"))
    assertFalse(fileService.containsFile(collectionId, "some/path/file.txt"))
    assertTrue(fileService.containsDirectory(collectionId, "new"))
    assertTrue(fileService.containsDirectory(collectionId, "new/inner"))
    assertTrue(fileService.containsFile(collectionId, "new/inner/file.txt"))
    assertTrue(equals(fileService.readFile(collectionId, "new/inner/file.txt").use { it.readBytes() }, byteArrayOf(42)))
    assertTrue(fileService.containsFile(collectionId, "new/file.txt"))
    assertTrue(equals(fileService.readFile(collectionId, "new/file.txt").use { it.readBytes() }, innerFile2Contents))
  }

  @Test
  fun `moving from child to parent directory`() {
    fileService.createDirectories(collectionId, setOf("some/path"))
    fileService.moveFile(collectionId, setOf("some/path"), "/")
    assertTrue(fileService.containsDirectory(collectionId, "path"))
  }

  @Test
  fun `ignore if renaming file to itself`() {
    fileService.createDirectories(collectionId, setOf("some"))
    fileService.createFiles(collectionId, setOf("some/file.txt"))
    fileService.renameFile(collectionId, "some/file.txt", "some/file.txt")
    assertTrue(fileService.containsFile(collectionId, "some/file.txt"))
  }

  @Test
  fun `ignore if file is moved to its own directory`() {
    fileService.createFiles(collectionId, setOf("file.txt"))
    fileService.createFiles(collectionId, setOf("file2.txt"))
    fileService.moveFile(collectionId, setOf("file.txt", "file2.txt"), "/")
    assertTrue(fileService.containsFile(collectionId, "file.txt"))
    assertTrue(fileService.containsFile(collectionId, "file2.txt"))

    fileService.createDirectories(collectionId, setOf("subdir"))
    fileService.createFiles(collectionId, setOf("subdir/file.txt"))
    fileService.moveFile(collectionId, setOf("subdir/file.txt"), "subdir")
    assertTrue(fileService.containsFile(collectionId, "subdir/file.txt"))
  }

  private fun <T> Stream<T>.find(criteria: (T) -> Boolean): T = use {
    this.filter(criteria)
      .findFirst()
      .orElse(null)
  }
}
