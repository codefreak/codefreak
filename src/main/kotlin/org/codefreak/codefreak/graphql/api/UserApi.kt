package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.graphql.BaseDto
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.graphql.ResolverContext
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@GraphQLName("User")
class UserDto(@GraphQLIgnore val entity: User, ctx: ResolverContext) : BaseDto(ctx) {
    @GraphQLID val id = entity.id
    val username = entity.username
    val firstName = entity.firstName
    val lastName = entity.lastName
    val roles by lazy {
      authorization.requireAuthorityIfNotCurrentUser(entity, Authority.ROLE_ADMIN)
      entity.roles.map { it.toString() }
    }
    val authorities by lazy {
      authorization.requireAuthorityIfNotCurrentUser(entity, Authority.ROLE_ADMIN)
      entity.authorities.map { it.authority }
    }
}

@Component
class UserQuery : BaseResolver(), Query {
  @PreAuthorize("isAuthenticated()")
  fun me() = context { UserDto(authorization.currentUser, this) }
}
