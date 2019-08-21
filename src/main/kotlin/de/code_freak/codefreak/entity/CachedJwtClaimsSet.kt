package de.code_freak.codefreak.entity

import com.nimbusds.jwt.JWTClaimsSet
import javax.persistence.Column
import javax.persistence.Entity

@Entity
class CachedJwtClaimsSet(
  @Column
  val jwtClaimSet: JWTClaimsSet
): BaseEntity()
