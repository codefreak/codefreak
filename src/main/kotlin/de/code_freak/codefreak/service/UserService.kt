package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService : BaseService() {
  @Autowired
  private lateinit var userRepository: UserRepository

  @Transactional
  fun getOrCreateUser(username: String, patch: User.() -> Unit): User {
    val user = userRepository.findByUsernameIgnoreCase(username).orElseGet { userRepository.save(User(username)) }
    user.patch()
    return user
  }
}
