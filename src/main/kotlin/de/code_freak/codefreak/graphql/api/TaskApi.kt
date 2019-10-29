package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import de.code_freak.codefreak.entity.Task
import de.code_freak.codefreak.graphql.ServiceAccess

@GraphQLName("Task")
class TaskDto(@GraphQLIgnore val entity: Task, @GraphQLIgnore val serviceAccess: ServiceAccess) {

  @GraphQLID
  val id = entity.id
  val title = entity.title
  val position = entity.position.toInt()
  val body = entity.body
  val assignment by lazy { AssignmentDto(entity.assignment, serviceAccess) }
}
