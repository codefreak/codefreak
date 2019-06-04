package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Task
import de.code_freak.codefreak.repository.TaskRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.UUID
import javax.transaction.Transactional

@Service
class TaskService {

  @Autowired
  private lateinit var taskRepository: TaskRepository

  @Transactional
  fun findTask(id: UUID): Task = taskRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Task not found") }
}
