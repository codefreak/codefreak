package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.auth.Authorization
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.graphql.BaseDto
import de.code_freak.codefreak.graphql.BaseResolver
import de.code_freak.codefreak.graphql.ResolverContext
import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.GitImportService
import de.code_freak.codefreak.service.evaluation.EvaluationService
import de.code_freak.codefreak.util.FrontendUtil
import de.code_freak.codefreak.util.TarUtil
import de.code_freak.codefreak.util.orNull
import org.apache.catalina.core.ApplicationPart
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@GraphQLName("Answer")
class AnswerDto(@GraphQLIgnore val entity: Answer, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val submission by lazy { SubmissionDto(entity.submission, ctx) }
  val task by lazy { TaskDto(entity.task, ctx) }
  val sourceUrl by lazy { FrontendUtil.getUriBuilder().path("/api/answers/$id/source").build().toUriString() }

  val latestEvaluation by lazy {
    serviceAccess.getService(EvaluationService::class)
        .getLatestEvaluation(id)
        .map { EvaluationDto(it, ctx) }
        .orNull()
  }

  val pendingEvaluation by lazy {
    if (serviceAccess.getService(EvaluationService::class).isEvaluationPending(id)) {
      PendingEvaluationDto(entity, ctx)
    } else {
      null
    }
  }

  val evaluations by lazy {
    entity.evaluations.sortedBy { it.createdAt }.map { EvaluationDto(it, ctx) }
  }

  val ideRunning by lazy {
    serviceAccess.getService(ContainerService::class)
        .isIdeContainerRunning(id)
  }
}

@Component
class AnswerMutation : BaseResolver(), Mutation {

  @Secured(Authority.ROLE_STUDENT)
  @Transactional
  fun uploadAnswerSource(id: UUID, files: Array<ApplicationPart>): Boolean = context {
    val answerService = serviceAccess.getService(AnswerService::class)
    val answer = answerService.findAnswer(id)
    Authorization().requireIsCurrentUser(answer.submission.user)
    answerService.setFiles(answer).use { TarUtil.writeUploadAsTar(files, it) }
    true
  }

  @Secured(Authority.ROLE_STUDENT)
  @Transactional
  fun importAnswerSource(id: UUID, url: String): Boolean = context {
    val answerService = serviceAccess.getService(AnswerService::class)
    val answer = answerService.findAnswer(id)
    Authorization().requireIsCurrentUser(answer.submission.user)
    serviceAccess.getService(AnswerService::class).setFiles(answer).use {
      serviceAccess.getService(GitImportService::class).importFiles(url, it)
    }
    true
  }

  @Secured(Authority.ROLE_STUDENT)
  @Transactional
  fun createAnswer(taskId: UUID): AnswerDto = context {
    serviceAccess.getService(AnswerService::class)
        .findOrCreateAnswer(taskId, FrontendUtil.getCurrentUser())
        .let { AnswerDto(it, this) }
  }
}

@Component
class AnswerQuery : BaseResolver(), Query {

  @Transactional
  @Secured(Authority.ROLE_STUDENT)
  fun answer(id: UUID): AnswerDto {
    return context {
      val answerService = serviceAccess.getService(AnswerService::class)
      val answer = answerService.findAnswer(id)
      if (!authorization.isCurrentUser(answer.submission.user)) {
        authorization.requireAuthority(Authority.ROLE_TEACHER)
      }
      AnswerDto(answer, this)
    }
  }
}
