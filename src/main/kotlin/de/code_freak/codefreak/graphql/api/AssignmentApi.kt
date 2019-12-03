package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.auth.Role
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.graphql.ServiceAccess
import de.code_freak.codefreak.service.AssignmentService
import de.code_freak.codefreak.service.SubmissionService
import de.code_freak.codefreak.util.FrontendUtil
import graphql.schema.DataFetchingEnvironment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@GraphQLName("Assignment")
class AssignmentDto(@GraphQLIgnore val entity: Assignment, @GraphQLIgnore val serviceAccess: ServiceAccess) {

  @GraphQLID
  val id = entity.id
  val title = entity.title
  val owner by lazy { UserDto(entity.owner) }
  val deadline = entity.deadline
  val closed = entity.closed
  val tasks by lazy { entity.tasks.map { TaskDto(it, serviceAccess) } }

  val submissions by lazy {
    serviceAccess.getService(SubmissionService::class)
        .findSubmissionsOfAssignment(id)
        .map { SubmissionDto(it, serviceAccess) }
  }
}

@Component
class AssignmentQuery : Query {

  @Autowired
  private lateinit var serviceAccess: ServiceAccess

  @Transactional
  @Secured(Authority.ROLE_STUDENT)
  fun assignments(env: DataFetchingEnvironment): List<AssignmentDto> {
    val assignmentService = serviceAccess.getService(AssignmentService::class)
    val user = FrontendUtil.getCurrentUser()
    val assignments = if (user.authorities.contains(SimpleGrantedAuthority(Authority.ROLE_TEACHER))) {
      assignmentService.findAllAssignments()
    } else {
      assignmentService.findAllAssignmentsForUser(user.id)
    }
    return assignments.map { AssignmentDto(it, serviceAccess) }
  }

  @Transactional
  fun assignment(id: UUID): AssignmentDto {
    return serviceAccess.getService(AssignmentService::class)
        .findAssignment(id)
        .let { AssignmentDto(it, serviceAccess) }
  }
}
