package org.codefreak.codefreak.entity

import javax.persistence.Column
import javax.persistence.Entity
import org.hibernate.annotations.Type

@Entity
class CachedJwtClaimsSet(
  @Column
  @Type(type = "text")
  var serializedClaimSet: String
) : BaseEntity()
