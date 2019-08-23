package de.code_freak.codefreak.entity

import com.nimbusds.jwt.JWTClaimsSet
import org.hibernate.annotations.Type
import javax.persistence.Column
import javax.persistence.Entity

@Entity
class CachedJwtClaimsSet(initClaimSet: JWTClaimsSet? = null) : BaseEntity() {
  @Column
  @Type(type = "text")
  var serializedClaimSet = initClaimSet.toString()

  @delegate:Transient
  val jwtClaimsSet: JWTClaimsSet by lazy {
    initClaimSet ?: JWTClaimsSet.parse(serializedClaimSet)
  }
}
