package org.codefreak.codefreak.service

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate

abstract class BaseService {
  @PersistenceContext
  protected lateinit var entityManager: EntityManager

  @Autowired
  private lateinit var transactionManager: PlatformTransactionManager

  protected fun <T> withNewTransaction(block: TransactionCallback<T>): T {
    val transactionTemplate = TransactionTemplate(transactionManager)
    transactionTemplate.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    return transactionTemplate.execute(block)!!
  }
}
