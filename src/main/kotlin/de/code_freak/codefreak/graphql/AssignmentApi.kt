package de.code_freak.codefreak.graphql

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.service.AssignmentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import java.util.UUID

@GraphQLName("Assignment")
class AssignmentDto(@GraphQLID val id: UUID, val title: String) {
  constructor(assignment: Assignment) : this(assignment.id, assignment.title)
}

@Component
class AssignmentQuery : Query {

  @Autowired(required = false)
  private lateinit var assignmentService: AssignmentService

  @Secured(Authority.ROLE_STUDENT)
  fun assignments(): List<AssignmentDto> {
    return assignmentService.findAllAssignments().map { AssignmentDto(it) }
  }
}
