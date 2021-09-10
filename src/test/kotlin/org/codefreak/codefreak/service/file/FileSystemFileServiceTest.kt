package org.codefreak.codefreak.service.file

import java.util.UUID
import org.codefreak.codefreak.config.AppConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class FileSystemFileServiceTest : FileServiceTest() {
  override var collectionId: UUID = UUID(0, 0)
  override lateinit var fileService: FileService

  @BeforeEach
  fun init() {
    val config = Mockito.mock(AppConfiguration::class.java)

    val files = Mockito.mock(AppConfiguration.Files::class.java)
    Mockito.`when`(config.files).thenReturn(files)

    val fileSystemConfig = Mockito.mock(AppConfiguration.Files.FileSystem::class.java)
    Mockito.`when`(files.fileSystem).thenReturn(fileSystemConfig)

    Mockito.`when`(fileSystemConfig.collectionStoragePath).thenReturn("/tmp/codefreak-test")

    fileService = FileSystemFileService(config)
  }

  @AfterEach
  fun tearDown() {
    // Cleanup created files
    fileService.deleteCollection(collectionId)
  }

  @Test
  fun `cannot read files outside of the collection`() {
    assertThrows(IllegalArgumentException::class.java) {
      fileService.readFile(collectionId, "/../foo.txt")
    }
    assertThrows(IllegalArgumentException::class.java) {
      fileService.readFile(collectionId, "foo/../../bar.txt")
    }
    assertThrows(IllegalArgumentException::class.java) {
      fileService.readFile(collectionId, "foo/../../../../../etc/passwd")
    }
  }

  @Test
  fun `files trying to escape the collection path are still created inside the collection`() {
    fileService.createFiles(collectionId, setOf("/../foo.txt", "foo/../../bar.txt"))

    assertTrue(fileService.containsFile(collectionId, "/foo.txt"))
    assertTrue(fileService.containsFile(collectionId, "/bar.txt"))
  }

  @Test
  fun `directories trying to escape the collection path are still created inside the collection`() {
    fileService.createDirectories(collectionId, setOf("/../foo", "foo/../../bar", "foo/../../../../../etc/passwd"))

    assertTrue(fileService.containsDirectory(collectionId, "/foo"))
    assertTrue(fileService.containsDirectory(collectionId, "/bar"))
    assertTrue(fileService.containsDirectory(collectionId, "/etc/passwd"))
  }

  @Test
  fun `cannot create blacklisted files and directories`() {
    assertThrows(IllegalArgumentException::class.java) {
      fileService.createFiles(collectionId, setOf(".git"))
    }
    assertThrows(IllegalArgumentException::class.java) {
      fileService.createFiles(collectionId, setOf(".git/foo"))
    }
    assertThrows(IllegalArgumentException::class.java) {
      fileService.createFiles(collectionId, setOf(".gitignore"))
    }
    assertThrows(IllegalArgumentException::class.java) {
      fileService.createFiles(collectionId, setOf(".gitattributes"))
    }
    assertThrows(IllegalArgumentException::class.java) {
      fileService.createDirectories(collectionId, setOf(".git"))
    }
    assertThrows(IllegalArgumentException::class.java) {
      fileService.createDirectories(collectionId, setOf(".git/foo"))
    }
    assertThrows(IllegalArgumentException::class.java) {
      fileService.createDirectories(collectionId, setOf(".gitignore"))
    }
    assertThrows(IllegalArgumentException::class.java) {
      fileService.createDirectories(collectionId, setOf(".gitattributes"))
    }
  }
}
