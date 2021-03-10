package org.codefreak.codefreak.repository

import org.codefreak.codefreak.entity.UserAlias
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository of UserAliasRepository. Associated generic functions added
 *
 */
@Repository
interface UserAliasRepository : CrudRepository<UserAlias, UUID> {
  fun findByUserId(id : UUID) : Optional<UserAlias>
  fun existsByAlias(alias : String) : Boolean
}
