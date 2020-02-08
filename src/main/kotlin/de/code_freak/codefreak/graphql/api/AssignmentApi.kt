package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.auth.hasAuthority
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.graphql.BaseDto
import de.code_freak.codefreak.graphql.BaseResolver
import de.code_freak.codefreak.graphql.ResolverContext
import de.code_freak.codefreak.service.AssignmentService
import de.code_freak.codefreak.service.SubmissionService
import de.code_freak.codefreak.util.FrontendUtil
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@GraphQLName("Assignment")
class AssignmentDto(@GraphQLIgnore val entity: Assignment, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val title = entity.title
  val owner by lazy { UserDto(entity.owner, ctx) }
  val createdAt = entity.createdAt
  val deadline = entity.deadline
  val closed = entity.closed
  val tasks by lazy { entity.tasks.map { TaskDto(it, ctx) } }
  val editable by lazy {
    authorization.isCurrentUser(entity.owner) || authorization.currentUser.hasAuthority(Authority.ROLE_ADMIN)
    // TODO depend on assignment status
  }

  val submissionCsvUrl by lazy { FrontendUtil.getUriBuilder().path("/api/assignments/$id/submissions.csv").build().toUriString() }
  val submissions by lazy {
    authorization.requireAuthority(Authority.ROLE_TEACHER)
    serviceAccess.getService(SubmissionService::class)
        .findSubmissionsOfAssignment(id)
        .map { SubmissionDto(it, ctx) }
  }
}

@Component
class AssignmentQuery : BaseResolver(), Query {

  @Transactional
  @Secured(Authority.ROLE_STUDENT)
  fun assignments(): List<AssignmentDto> = context {
    val assignmentService = serviceAccess.getService(AssignmentService::class)
    val user = FrontendUtil.getCurrentUser()
    val assignments = if (user.authorities.contains(SimpleGrantedAuthority(Authority.ROLE_TEACHER))) {
      assignmentService.findAllAssignments()
    } else {
      assignmentService.findAllAssignmentsForUser(user.id)
    }
    assignments.map { AssignmentDto(it, this) }
  }

  @Transactional
  fun assignment(id: UUID): AssignmentDto = context {
    serviceAccess.getService(AssignmentService::class)
        .findAssignment(id)
        .let { AssignmentDto(it, this) }
  }
}
