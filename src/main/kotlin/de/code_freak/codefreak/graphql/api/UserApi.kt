package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.util.FrontendUtil
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import java.util.UUID

@GraphQLName("User")
class UserDto(@GraphQLID val id: UUID, val username: String, val roles: List<String>) {
  // TODO: only show roles to admins and the user itself
  constructor(user: User) : this(user.id, user.username, user.roles.map { it.toString() })
}

@Component
class UserQuery : Query {
  @PreAuthorize("isAuthenticated()")
  fun me() = UserDto(FrontendUtil.getCurrentUser())
}
