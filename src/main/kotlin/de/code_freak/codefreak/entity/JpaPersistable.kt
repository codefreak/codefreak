package de.code_freak.codefreak.entity

import java.io.Serializable
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class JpaPersistable<T : Serializable>(
  @Id
  @GeneratedValue
  var id: T? = null
) {

  override fun equals(other: Any?): Boolean {
    other ?: return false

    if (this === other) return true

    if (this::class != other::class) return false

    other as JpaPersistable<*>
    return if (null == this.id) false else this.id == other.id
  }
}
