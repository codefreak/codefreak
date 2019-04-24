package de.code_freak.codefreak.service

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

abstract class BaseService {
  @PersistenceContext
  protected lateinit var entityManager: EntityManager
}
