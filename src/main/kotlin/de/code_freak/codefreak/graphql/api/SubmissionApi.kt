package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.graphql.ServiceAccess
import de.code_freak.codefreak.service.SubmissionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@GraphQLName("Submission")
class SubmissionDto(@GraphQLIgnore val entity: Submission, @GraphQLIgnore val serviceAccess: ServiceAccess) {

  @GraphQLID
  val id = entity.id
  val user by lazy { UserDto(entity.user) }
  val assignment by lazy { AssignmentDto(entity.assignment, serviceAccess) }
  val answers by lazy { entity.answers.map { AnswerDto(it, serviceAccess) } }
}

@Component
class SubmissionQuery : Query {

  @Autowired
  private lateinit var serviceAccess: ServiceAccess

  @Transactional
  @Secured(Authority.ROLE_TEACHER)
  fun submissions(assignmentId: UUID): List<SubmissionDto> {
    val submissionService = serviceAccess.getService(SubmissionService::class)
    return submissionService.findSubmissionsOfAssignment(assignmentId).map { submission ->
      SubmissionDto(submission, serviceAccess)
    }
  }
}
