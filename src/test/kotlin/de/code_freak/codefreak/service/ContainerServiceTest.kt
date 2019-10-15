package de.code_freak.codefreak.service

import com.nhaarman.mockitokotlin2.eq
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.ListContainersParam
import de.code_freak.codefreak.SpringTest
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.TarUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayOutputStream
import java.util.UUID

internal class ContainerServiceTest : SpringTest() {

  @MockBean
  lateinit var fileService: FileService

  @Autowired
  lateinit var docker: DockerClient

  @Autowired
  lateinit var containerService: ContainerService

  val answer: Answer by lazy {
    val mock = mock(Answer::class.java, Mockito.RETURNS_DEEP_STUBS)
    `when`(mock.id).thenReturn(UUID(0, 0))
    mock
  }

  private val files = ByteArrayOutputStream().use {
    TarUtil.createTarFromDirectory(ClassPathResource("tasks/c-simple").file, it); it.toByteArray()
  }

  @Before
  @After
  fun tearDown() {
    // delete all containers before and after each run
    getAllIdeContainers().parallelStream().forEach {
      docker.killContainer(it.id())
      docker.removeContainer(it.id())
    }
  }

  @Test
  fun `New IDE container is started`() {
    containerService.startIdeContainer(answer)
    val container = getIdeContainer(answer) // throws if container is not present
    assertTrue(docker.inspectContainer(container.id()).state().running())
  }

  @Test
  fun `Existing IDE container is used`() {
    containerService.startIdeContainer(answer)
    containerService.startIdeContainer(answer) // start twice for the same answer
    assertThat(getIdeContainers(answer), hasSize(1))
  }

  @Test
  fun `files are extracted to project directory`() {
    `when`(fileService.readCollectionTar(eq(answer.id))).thenReturn(files.inputStream())
    `when`(fileService.collectionExists(eq(answer.id))).thenReturn(true)
    containerService.startIdeContainer(answer)
    val containerId = getIdeContainer(answer).id()
    // assert that file is existing and nothing is owned by root
    val dirContent = containerService.exec(containerId, arrayOf("ls", "-l", ContainerService.PROJECT_PATH))
    assertThat(dirContent.output, containsString("main.c"))
    assertThat(dirContent.output, not(containsString("root")))
  }

  @Test
  fun `files in the container are saved back to the database`() {
    val out = ByteArrayOutputStream()
    `when`(fileService.readCollectionTar(eq(answer.id))).thenReturn(files.inputStream())
    `when`(fileService.collectionExists(eq(answer.id))).thenReturn(true)
    `when`(fileService.writeCollectionTar(eq(answer.id))).thenReturn(out)
    `when`(answer.task.assignment.closed).thenReturn(false)
    containerService.startIdeContainer(answer)
    containerService.saveAnswerFiles(answer)
    //verify(fileService, times(1)).writeCollectionTar(answer.id)
    assertThat(out.toByteArray().size, greaterThan(0))
  }

  @Test
  fun `files are not overridden in existing IDE containers`() {
    `when`(fileService.readCollectionTar(eq(answer.id))).thenReturn(files.inputStream())
    `when`(fileService.collectionExists(eq(answer.id))).thenReturn(true)
    containerService.startIdeContainer(answer)
    val containerId = getIdeContainer(answer).id()
    containerService.exec(containerId, arrayOf("sh", "-c", "echo 'foo' >> main.c"))
    val fileContentBefore = containerService.exec(containerId, arrayOf("cat", "main.c"))
    containerService.startIdeContainer(answer)
    val fileContentAfter = containerService.exec(containerId, arrayOf("cat", "main.c"))
    assertThat(fileContentAfter, `is`(fileContentBefore))
  }

  @Test
  @Ignore("Disabled until #138 is fixed")
  fun `idle containers are shut down automatically`() {
    containerService.startIdeContainer(answer)
    Thread.sleep(10000)
    assertThat(getIdeContainers(answer), hasSize(0))
  }

  private fun getAllIdeContainers() = docker.listContainers(
      ListContainersParam.withLabel(ContainerService.LABEL_ANSWER_ID)
  )

  private fun getIdeContainers(answer: Answer) = docker.listContainers(
      ListContainersParam.withLabel(ContainerService.LABEL_ANSWER_ID, answer.id.toString())
  )

  private fun getIdeContainer(answer: Answer) = getIdeContainers(answer).first()
}
