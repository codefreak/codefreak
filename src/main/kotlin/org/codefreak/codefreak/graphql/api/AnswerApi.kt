package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import java.util.UUID
import org.apache.catalina.core.ApplicationPart
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.auth.Authorization
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.graphql.BaseDto
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.graphql.ResolverContext
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.GitImportService
import org.codefreak.codefreak.service.SubmissionService
import org.codefreak.codefreak.service.evaluation.EvaluationService
import org.codefreak.codefreak.util.FrontendUtil
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.orNull
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@GraphQLName("Answer")
class AnswerDto(@GraphQLIgnore val entity: Answer, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val submission by lazy { SubmissionDto(entity.submission, ctx) }
  val task by lazy { TaskDto(entity.task, ctx) }
  val sourceUrl by lazy { FrontendUtil.getUriBuilder().path("/api/answers/$id/source").build().toUriString() }
  val createdAt = entity.createdAt
  val updatedAt = entity.updatedAt

  val latestEvaluation by lazy {
    serviceAccess.getService(EvaluationService::class)
        .getLatestEvaluation(id)
        .map { EvaluationDto(it, ctx) }
        .orNull()
  }

  val evaluations by lazy {
    entity.evaluations.sortedBy { it.createdAt }.map { EvaluationDto(it, ctx) }
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

  @Secured(Authority.ROLE_TEACHER)
  fun deleteAnswer(id: UUID): Boolean = context {
    val answer = serviceAccess.getService(AnswerService::class).findAnswer(id)
    authorization.requireAuthorityIfNotCurrentUser(answer.submission.user, Authority.ROLE_ADMIN)
    check(!serviceAccess.getService(EvaluationService::class).isEvaluationScheduled(answer.id)) {
      "Answer cannot be deleted while evaluation is running"
    }
    // If this is the only answer, delete the whole submission. This makes sense for testing mode.
    // We may want to change this, if students are ever able to delete answers.
    if (answer.submission.answers.size == 1) {
      serviceAccess.getService(SubmissionService::class).deleteSubmission(answer.submission.id)
    } else {
      serviceAccess.getService(AnswerService::class).deleteAnswer(answer.id)
    }
    true
  }

  @Secured(Authority.ROLE_STUDENT)
  fun resetAnswer(id: UUID): Boolean = context {
    val answerService = serviceAccess.getService(AnswerService::class)
    val answer = answerService.findAnswer(id)
    authorization.requireIsCurrentUser(answer.submission.user)
    answerService.resetAnswerFiles(answer)
    true
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
