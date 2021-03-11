package org.codefreak.codefreak.repository

import java.util.Optional
import java.util.UUID
import org.codefreak.codefreak.entity.UserAlias
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

/**
 * Repository of UserAliasRepository. Associated generic functions added
 *
 */
@Repository
interface UserAliasRepository : CrudRepository<UserAlias, UUID> {
  fun findByUserId(id: UUID): Optional<UserAlias>
  fun existsByAlias(alias: String): Boolean
}
