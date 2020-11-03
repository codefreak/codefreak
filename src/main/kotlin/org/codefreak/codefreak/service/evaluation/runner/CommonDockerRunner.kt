package org.codefreak.codefreak.service.evaluation.runner

import com.spotify.docker.client.DockerClient.ListContainersParam
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.service.ContainerService
import org.codefreak.codefreak.service.evaluation.StoppableEvaluationRunner
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class CommonDockerRunner : StoppableEvaluationRunner {

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
    log.debug("Stopping container $containerId running step ${getName()} for answer ${answer.id}")
    containerService.stopContainer(containerId, 2)
  }

  private fun getEvalContainerListParams(answer: Answer) = getContainerLabelMap(answer).entries.map { (key, value) ->
    ListContainersParam.withLabel(key, value)
  }.toTypedArray()

  protected fun getContainerLabelMap(answer: Answer) = mapOf(
      ContainerService.LABEL_PREFIX + "eval.runner" to getName(),
      ContainerService.LABEL_PREFIX + "eval.answer-id" to answer.id.toString()
  )
}
