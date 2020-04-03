package org.codefreak.codefreak.repository

import org.codefreak.codefreak.entity.CachedJwtClaimsSet
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CachedJwtClaimsSetRepository : CrudRepository<CachedJwtClaimsSet, UUID>
