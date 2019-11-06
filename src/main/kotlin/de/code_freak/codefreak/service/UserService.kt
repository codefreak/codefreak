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
    val user = try {
      getUser(username)
    } catch (e: EntityNotFoundException) {
      userRepository.save(User(username))
    }
    user.patch()
    return user
  }

  fun getUser(username: String): User = detached {
    userRepository.findByUsernameCanonical(username.toLowerCase())
        .orElseThrow { EntityNotFoundException("User cannot be found") }
  }
}
