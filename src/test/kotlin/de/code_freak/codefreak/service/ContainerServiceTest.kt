package de.code_freak.codefreak.service

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.ListContainersParam
import de.code_freak.codefreak.config.DockerConfiguration
import de.code_freak.codefreak.entity.TaskSubmission
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.util.UUID

@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = [DockerConfiguration::class, ContainerService::class])
@ActiveProfiles("test")
internal class ContainerServiceTest {

  @Autowired
  lateinit var docker: DockerClient

  @Autowired
  lateinit var containerService: ContainerService

  val taskSubmission by lazy {
    val mock = mock(TaskSubmission::class.java)
    `when`(mock.id).thenReturn(UUID(0,0))
    mock
  }

  @After
  fun tearDown() {
    // delete all containers after each run
    listIdeContainer().parallelStream().forEach {
      docker.killContainer(it.id())
      docker.removeContainer(it.id())
    }
  }

  @Test
  fun `New IDE container is started`() {
    val containerId = containerService.startIdeContainer(taskSubmission)
    val containers = listIdeContainer()
    assertThat(containers, hasSize(1))
    assertThat(containers[0].id(), equalTo(containerId))
  }

  @Test
  fun `Existing IDE container is used`() {
    val containerId1 = containerService.startIdeContainer(taskSubmission)
    containerService.startIdeContainer(taskSubmission)
    val containers = listIdeContainer()
    assertThat(containers, hasSize(1))
    assertThat(containers[0].id(), equalTo(containerId1))
  }

  private fun listIdeContainer() = docker.listContainers(
    ListContainersParam.withLabel(ContainerService.LABEL_TASK_SUBMISSION_ID)
  )
}
