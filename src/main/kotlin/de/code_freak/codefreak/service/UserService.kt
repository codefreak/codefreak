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

  /**
   * Retrieve or create a user based on his username
   * Accepts an optional function to make changes to the user
   *
   * @param username Username of the user
   * @param patch Optional function to modify the user. Will change new AND existing users
   */
  @Transactional
  fun getOrCreateUser(username: String, patch: User.() -> Unit = {}): User {
    val user: User = userRepository.findByUsernameCanonical(username.toLowerCase()).orElseGet {
      userRepository.save(User(username))
    }
    user.patch()
    return user
  }

  @Transactional(readOnly = true)
  fun getUser(username: String): User = userRepository.findByUsernameCanonical(username.toLowerCase()).orElseThrow {
    EntityNotFoundException("User cannot be found")
  }
}
