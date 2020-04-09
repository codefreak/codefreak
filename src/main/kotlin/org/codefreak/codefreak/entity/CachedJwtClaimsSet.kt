package org.codefreak.codefreak.entity

import org.hibernate.annotations.Type
import javax.persistence.Column
import javax.persistence.Entity

@Entity
class CachedJwtClaimsSet(
  @Column
  @Type(type = "text")
  var serializedClaimSet: String
) : BaseEntity()
