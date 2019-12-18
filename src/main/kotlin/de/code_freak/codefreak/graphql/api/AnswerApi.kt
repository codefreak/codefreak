package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.graphql.ServiceAccess
import de.code_freak.codefreak.service.evaluation.EvaluationService
import de.code_freak.codefreak.util.orNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@GraphQLName("Answer")
class AnswerDto(@GraphQLIgnore val entity: Answer, @GraphQLIgnore val serviceAccess: ServiceAccess) {

  @GraphQLID
  val id = entity.id
  val submission by lazy { SubmissionDto(entity.submission, serviceAccess) }
  val task by lazy { TaskDto(entity.task, serviceAccess) }

  val latestEvaluation by lazy {
    serviceAccess.getService(EvaluationService::class)
        .getLatestEvaluation(id)
        .map { EvaluationDto(it, serviceAccess) }
        .orNull()
  }
}

@Component
class AnswerQuery : Query {

  @Autowired
  private lateinit var serviceAccess: ServiceAccess
}
