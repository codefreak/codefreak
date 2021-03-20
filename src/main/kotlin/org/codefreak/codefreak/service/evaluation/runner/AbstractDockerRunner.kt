package org.codefreak.codefreak.service.evaluation.runner

import com.spotify.docker.client.DockerClient.ListContainersParam
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.service.ContainerService
import org.codefreak.codefreak.service.evaluation.StoppableEvaluationRunner
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractDockerRunner : StoppableEvaluationRunner {

  @Autowired
  private lateinit var containerService: ContainerService

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun stop(answer: Answer) {
    val containerId = containerService.listContainers(*getEvalContainerListParams(answer)).firstOrNull()?.id()
    if (containerId == null) {
      // container has already exited
      log.debug("Cannot find any evaluation container of type ${getName()} for answer ${answer.id}")
      return
    }
    log.debug("Removing container $containerId running step ${getName()} for answer ${answer.id}")
    containerService.removeContainer(containerId, force = true, removeVolumes = true)
  }

  private fun getEvalContainerListParams(answer: Answer) = getContainerLabelMap(answer).entries.map { (key, value) ->
    ListContainersParam.withLabel(key, value)
  }.toTypedArray()

  protected fun getContainerLabelMap(answer: Answer) = mapOf(
      ContainerService.LABEL_PREFIX + "eval.runner" to getName(),
      ContainerService.LABEL_PREFIX + "eval.answer-id" to answer.id.toString()
  )

  protected fun buildEnvVariables(answer: Answer): List<String> {
    val submission = answer.submission
    val user = submission.user
    return listOf(
        "CI=true",
        "CODEFREAK_USER_USERNAME=${user.usernameCanonical}",
        "CODEFREAK_USER_FIRST_NAME=${user.firstName}",
        "CODEFREAK_USER_LAST_NAME=${user.lastName}",
        "CODEFREAK_USER_ID=${user.id}",
        "CODEFREAK_ANSWER_ID=${answer.id}",
        "CODEFREAK_TASK_ID=${answer.task.id}",
        "CODEFREAK_SUBMISSION_ID=${submission.id}",
        "CODEFREAK_ASSIGNMENT_ID=${submission.assignment?.id ?: ""}"
    )
  }
}
