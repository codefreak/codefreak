package de.code_freak.codefreak.entity

import java.util.UUID
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class BaseEntity(
  @Id
  var id: UUID = UUID.randomUUID()
) {

  override fun equals(other: Any?): Boolean {
    other ?: return false

    if (this === other) return true

    if (this::class != other::class) return false

    other as BaseEntity
    return this.id == other.id
  }

  override fun hashCode(): Int {
    return this.id.hashCode()
  }
}
