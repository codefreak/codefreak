package de.code_freak.codefreak.repository

import de.code_freak.codefreak.entity.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserRepository : CrudRepository<User, UUID> {
  fun findByUsernameIgnoreCase(username: String): Optional<User>
}
