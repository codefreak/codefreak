package org.codefreak.codefreak.service.file

import java.util.UUID
import org.codefreak.codefreak.config.AppConfiguration
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class FileSystemFileServiceTest : FileServiceTest() {
  override var collectionId: UUID = UUID(0, 0)
  override lateinit var fileService: FileService

  @Before
  fun init() {
    val config = Mockito.mock(AppConfiguration::class.java)

    val files = Mockito.mock(AppConfiguration.Files::class.java)
    Mockito.`when`(config.files).thenReturn(files)

    val fileSystemConfig = Mockito.mock(AppConfiguration.Files.FileSystem::class.java)
    Mockito.`when`(files.fileSystem).thenReturn(fileSystemConfig)

    Mockito.`when`(fileSystemConfig.collectionStoragePath).thenReturn("/tmp/codefreak-test")

    fileService = FileSystemFileService(config)
  }

  @After
  fun tearDown() {
    // Cleanup created file
    fileService.deleteCollection(collectionId)
  }

  @Test
  fun `cannot read files outside of the collection`() {
    try {
      fileService.readFile(collectionId, "/../foo.txt")
      Assert.fail()
    } catch (e: IllegalArgumentException) {}

    try {
      fileService.readFile(collectionId, "foo/../../bar.txt")
      Assert.fail()
    } catch (e: IllegalArgumentException) {}

    try {
      fileService.readFile(collectionId, "foo/../../../../../etc/passwd")
      Assert.fail()
    } catch (e: IllegalArgumentException) {}
  }

  @Test
  fun `files trying to escape the collection path are still created inside the collection`() {
    fileService.createFiles(collectionId, setOf("/../foo.txt", "foo/../../bar.txt"))

    Assert.assertTrue(fileService.containsFile(collectionId, "/foo.txt"))
    Assert.assertTrue(fileService.containsFile(collectionId, "/bar.txt"))

    Assert.assertThrows(IllegalArgumentException::class.java) {
      // /etc does not exist
      fileService.createFiles(collectionId, setOf("foo/../../../../../etc/passwd"))
    }
  }

  @Test
  fun `directories trying to escape the collection path are still created inside the collection`() {
    fileService.createDirectories(collectionId, setOf("/../foo", "foo/../../bar", "foo/../../../../../etc/passwd"))

    Assert.assertTrue(fileService.containsDirectory(collectionId, "/foo"))
    Assert.assertTrue(fileService.containsDirectory(collectionId, "/bar"))
    Assert.assertTrue(fileService.containsDirectory(collectionId, "/etc/passwd"))
  }

  @Test
  fun `cannot create blacklisted files and directories`() {
    try {
      fileService.createFiles(collectionId, setOf(".git"))
      Assert.fail()
    } catch (e: IllegalArgumentException) {}

    try {
      fileService.createFiles(collectionId, setOf(".git/foo"))
      Assert.fail()
    } catch (e: IllegalArgumentException) {}

    try {
      fileService.createFiles(collectionId, setOf(".gitignore"))
      Assert.fail()
    } catch (e: IllegalArgumentException) {}

    try {
      fileService.createFiles(collectionId, setOf(".gitattributes"))
      Assert.fail()
    } catch (e: IllegalArgumentException) {}

    try {
      fileService.createDirectories(collectionId, setOf(".git"))
      Assert.fail()
    } catch (e: IllegalArgumentException) {}

    try {
      fileService.createDirectories(collectionId, setOf(".git/foo"))
      Assert.fail()
    } catch (e: IllegalArgumentException) {}

    try {
      fileService.createDirectories(collectionId, setOf(".gitignore"))
      Assert.fail()
    } catch (e: IllegalArgumentException) {}

    try {
      fileService.createDirectories(collectionId, setOf(".gitattributes"))
      Assert.fail()
    } catch (e: IllegalArgumentException) {}
  }
}
