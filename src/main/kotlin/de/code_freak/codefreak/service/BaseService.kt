package de.code_freak.codefreak.service

import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

abstract class BaseService {
  @PersistenceContext
  protected lateinit var entityManager: EntityManager

  @Transactional(noRollbackFor = [Throwable::class])
  protected open fun <T> noRollbackOnError(block: () -> T) = block()
}
