package org.codefreak.codefreak.service.file

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import java.nio.file.Paths
import java.util.UUID
import org.codefreak.codefreak.config.AppConfiguration
import org.junit.After
import org.junit.Before
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito

class FileSystemFileServiceTest : FileServiceTest() {
  override var collectionId: UUID = UUID(0, 0)
  override lateinit var fileService: FileService
  lateinit var pathsMock: MockedStatic<Paths>

  @Before
  fun init() {
    val config = Mockito.mock(AppConfiguration::class.java)

    val files = Mockito.mock(AppConfiguration.Files::class.java)
    Mockito.`when`(config.files).thenReturn(files)

    val fileSystemConfig = Mockito.mock(AppConfiguration.Files.FileSystem::class.java)
    Mockito.`when`(files.fileSystem).thenReturn(fileSystemConfig)

    Mockito.`when`(fileSystemConfig.collectionStoragePath).thenReturn("/var/lib/codefreak")

    val jimfsConfiguration = Configuration.unix().toBuilder()
      .setAttributeViews("basic", "owner", "posix", "unix")
      .build()
    val fileSystem = Jimfs.newFileSystem(jimfsConfiguration)

    pathsMock = Mockito.mockStatic(Paths::class.java)
    Mockito.`when`(Paths.get(anyString(), anyString())).thenAnswer {
      fileSystem.getPath(it.arguments[0] as String, it.arguments[1] as String)
    }

    fileService = FileSystemFileService(config)
  }

  @After
  fun tearDown() {
    // Cleanup created files
    fileService.deleteCollection(collectionId)
    // Cleanup filesystem mock
    pathsMock.close()
  }
}
