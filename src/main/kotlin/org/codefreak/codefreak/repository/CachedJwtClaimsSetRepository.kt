package org.codefreak.codefreak.repository

import java.util.UUID
import org.codefreak.codefreak.entity.CachedJwtClaimsSet
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CachedJwtClaimsSetRepository : CrudRepository<CachedJwtClaimsSet, UUID>
