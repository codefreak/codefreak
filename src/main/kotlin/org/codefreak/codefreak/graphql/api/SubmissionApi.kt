package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import org.codefreak.codefreak.entity.Submission
import org.codefreak.codefreak.graphql.BaseDto
import org.codefreak.codefreak.graphql.ResolverContext

@GraphQLName("Submission")
class SubmissionDto(@GraphQLIgnore val entity: Submission, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val user by lazy { UserDto(entity.user, ctx) }
  val assignment by lazy { entity.assignment?.let { AssignmentDto(it, ctx) } }
  val answers by lazy { entity.answers.map { AnswerDto(it, ctx) } }
}
