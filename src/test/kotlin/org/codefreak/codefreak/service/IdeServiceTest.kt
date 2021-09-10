package org.codefreak.codefreak.service

import com.nhaarman.mockitokotlin2.eq
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.ListContainersParam
import java.io.ByteArrayOutputStream
import org.codefreak.codefreak.SpringTest
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.TarUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.not
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.ClassPathResource

/**
 * The Docker library has some issues on Windows so we skip
 * these tests on this platform.
 */
@EnabledOnOs(OS.LINUX)
internal class IdeServiceTest : SpringTest() {

  @MockBean
  lateinit var fileService: FileService

  @Autowired
  lateinit var docker: DockerClient

  @Autowired
  lateinit var ideService: IdeService

  @Autowired
  lateinit var containerService: ContainerService

  private val files = ByteArrayOutputStream().use {
    TarUtil.createTarFromDirectory(ClassPathResource("tasks/c-simple").file, it); it.toByteArray()
  }

  @BeforeEach
  fun setupEntities() = super.seedDatabase()

  @AfterEach
  fun removeEntities() = super.clearDatabase()

  @BeforeEach
  @AfterEach
  fun tearDown() {
    // delete all containers before and after each run
    getAllIdeContainers().parallelStream().forEach {
      docker.killContainer(it.id())
      docker.removeContainer(it.id())
    }
  }

  @Test
  fun `New IDE container is started`() {
    ideService.startIdeContainer(answer)
    val container = getIdeContainer(answer) // throws if container is not present
    assertTrue(docker.inspectContainer(container.id()).state().running())
  }

  @Test
  fun `Existing IDE container is used`() {
    ideService.startIdeContainer(answer)
    ideService.startIdeContainer(answer) // start twice for the same answer
    assertThat(getIdeContainers(answer), hasSize(1))
  }

  @Test
  fun `files are extracted to project directory`() {
    `when`(fileService.readCollectionTar(eq(answer.id))).thenReturn(files.inputStream())
    `when`(fileService.collectionExists(eq(answer.id))).thenReturn(true)
    ideService.startIdeContainer(answer)
    val containerId = getIdeContainer(answer).id()
    // assert that file is existing and nothing is owned by root
    val dirContent = containerService.exec(containerId, arrayOf("ls", "-l", IdeService.PROJECT_PATH))
    assertThat(dirContent.output, containsString("main.c"))
    assertThat(dirContent.output, not(containsString("root")))
  }

  @Test
  fun `files in the container are saved back to the database`() {
    val out = ByteArrayOutputStream()
    `when`(fileService.readCollectionTar(eq(answer.id))).thenReturn(files.inputStream())
    `when`(fileService.collectionExists(eq(answer.id))).thenReturn(true)
    `when`(fileService.writeCollectionTar(eq(answer.id))).thenReturn(out)
    ideService.startIdeContainer(answer)
    ideService.saveAnswerFiles(answer)
    // verify(fileService, times(1)).writeCollectionTar(answer.id)
    assertThat(out.toByteArray().size, greaterThan(0))
  }

  @Test
  fun `files are not overridden in existing IDE containers`() {
    `when`(fileService.readCollectionTar(eq(answer.id))).thenReturn(files.inputStream())
    `when`(fileService.collectionExists(eq(answer.id))).thenReturn(true)
    ideService.startIdeContainer(answer)
    val containerId = getIdeContainer(answer).id()
    containerService.exec(containerId, arrayOf("sh", "-c", "echo 'foo' >> main.c"))
    val fileContentBefore = containerService.exec(containerId, arrayOf("cat", "main.c"))
    ideService.startIdeContainer(answer)
    val fileContentAfter = containerService.exec(containerId, arrayOf("cat", "main.c"))
    assertThat(fileContentAfter, `is`(fileContentBefore))
  }

  @Test
  @Disabled("Disabled until #138 is fixed")
  fun `idle containers are shut down automatically`() {
    ideService.startIdeContainer(answer)
    Thread.sleep(10000)
    assertThat(getIdeContainers(answer), hasSize(0))
  }

  private fun getAllIdeContainers() = docker.listContainers(
      ListContainersParam.withLabel(IdeService.LABEL_ANSWER_ID)
  )

  private fun getIdeContainers(answer: Answer) = docker.listContainers(
      ListContainersParam.withLabel(IdeService.LABEL_ANSWER_ID, answer.id.toString())
  )

  private fun getIdeContainer(answer: Answer) = getIdeContainers(answer).first()
}
