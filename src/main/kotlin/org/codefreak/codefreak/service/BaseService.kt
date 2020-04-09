package org.codefreak.codefreak.service

import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

abstract class BaseService {
  @PersistenceContext
  protected lateinit var entityManager: EntityManager

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  open fun <T> withNewTransaction(block: () -> T) = block()
}
