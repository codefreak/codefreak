package org.codefreak.codefreak.entity

import java.util.UUID
import javax.persistence.Entity
import org.hibernate.annotations.Type

@Entity
class FileCollection(id: UUID) : BaseEntity() {

  init {
      this.id = id
  }

  @Type(type = "image")
  var tar: ByteArray = byteArrayOf()
}
