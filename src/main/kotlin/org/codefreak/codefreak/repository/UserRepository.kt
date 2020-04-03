package org.codefreak.codefreak.repository

import org.codefreak.codefreak.entity.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserRepository : CrudRepository<User, UUID> {
  fun findByUsernameCanonical(username: String): Optional<User>
}
