package org.codefreak.codefreak.entity

import org.hibernate.annotations.Type
import java.util.UUID
import javax.persistence.Entity

@Entity
class FileCollection(id: UUID) : BaseEntity() {

  init {
      this.id = id
  }

  @Type(type = "image")
  var tar: ByteArray = byteArrayOf()
}
