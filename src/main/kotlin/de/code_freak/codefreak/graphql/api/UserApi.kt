package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.util.FrontendUtil
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@GraphQLName("User")
class UserDto(@GraphQLIgnore val entity: User) {
    @GraphQLID val id = entity.id
    val username = entity.username
    // TODO: only show roles/authority to admins and the user itself
    val roles by lazy { entity.roles.map { it.toString() } }
    val authorities by lazy { entity.authorities.map { it.authority } }
}

@Component
class UserQuery : Query {
  @PreAuthorize("isAuthenticated()")
  fun me() = UserDto(FrontendUtil.getCurrentUser())
}
