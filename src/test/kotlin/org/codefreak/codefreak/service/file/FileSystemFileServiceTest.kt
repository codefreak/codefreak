package org.codefreak.codefreak.service.file

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.codefreak.codefreak.config.AppConfiguration
import org.junit.After
import org.junit.Before
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import java.nio.file.Paths
import java.util.UUID

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

    val fileSystem = Jimfs.newFileSystem(Configuration.unix())
    pathsMock = Mockito.mockStatic(Paths::class.java)
    Mockito.`when`(Paths.get(anyString(), anyString())).thenAnswer {
      fileSystem.getPath(it.arguments[0] as String, it.arguments[1] as String)
    }

    fileService = FileSystemFileService(config)
  }

  @After
  fun tearDown() {
    pathsMock.close()
  }
}
