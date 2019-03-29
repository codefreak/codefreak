package de.code_freak.codefreak.service

import de.code_freak.codefreak.repository.AssignmentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.UUID
import javax.transaction.Transactional

@Service
class AssignmentManager {
  @Autowired
  lateinit var assignmentRepository: AssignmentRepository

  @Transactional
  fun findAssignment(id: UUID) = assignmentRepository.findById(id)
}
