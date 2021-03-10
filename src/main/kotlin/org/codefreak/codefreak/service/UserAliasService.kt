package org.codefreak.codefreak.service

import org.codefreak.codefreak.entity.UserAlias
import org.codefreak.codefreak.repository.UserAliasRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service for all processes regarding the  UserAlias Entity
 */
@Service
class UserAliasService : BaseService() {

  @Autowired
  private lateinit var userAliasRepository: UserAliasRepository

  fun getByUserId(id : UUID) : UserAlias = userAliasRepository.findByUserId(id).get()

  fun getById(id : UUID) : UserAlias = userAliasRepository.findById(id).get()

  fun save(userAlias : UserAlias) = userAliasRepository.save(userAlias)

  fun existsByAlias(alias : String) = userAliasRepository.existsByAlias(alias)

}
