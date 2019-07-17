package de.code_freak.codefreak.frontend

import com.hsingh.shortuuid.ShortUuid
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.BaseEntity
import de.code_freak.codefreak.entity.Task
import org.springframework.stereotype.Component

@Component
class Urls {
  private val shortUuidBuilder = ShortUuid.Builder()

  private val BaseEntity.shortId get() = shortUuidBuilder.build(id).toString()

  fun get(task: Task) = "/tasks/" + task.shortId

  fun get(assignment: Assignment) = "/assignments/" + assignment.shortId
}
