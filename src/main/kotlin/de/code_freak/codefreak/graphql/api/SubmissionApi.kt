package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.graphql.ServiceAccess

@GraphQLName("Submission")
class SubmissionDto(@GraphQLIgnore val entity: Submission, @GraphQLIgnore val serviceAccess: ServiceAccess) {

  @GraphQLID
  val id = entity.id
  val user by lazy { UserDto(entity.user) }
  val assignment by lazy { AssignmentDto(entity.assignment, serviceAccess) }
  val answers by lazy { entity.answers.map { AnswerDto(it, serviceAccess) } }
}
