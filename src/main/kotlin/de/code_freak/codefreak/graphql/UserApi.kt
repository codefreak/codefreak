package de.code_freak.codefreak.graphql

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLName
import de.code_freak.codefreak.entity.User
import java.util.UUID

@GraphQLName("User")
class UserDto(@GraphQLID val id: UUID, val username: String) {
  constructor(user: User) : this(user.id, user.username)
}
