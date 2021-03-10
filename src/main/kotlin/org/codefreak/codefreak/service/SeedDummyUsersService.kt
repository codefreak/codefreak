package org.codefreak.codefreak.service

import org.codefreak.codefreak.auth.Role
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

/**
 * Seed some dummy users until we have a proper UI for managing user
 * This must run before seeding sample data
 */
@Service
@ConditionalOnProperty("codefreak.authentication-method", havingValue = "simple", matchIfMissing = true)
@Order(Ordered.LOWEST_PRECEDENCE - 1)
class SeedDummyUsersService : ApplicationListener<ContextRefreshedEvent> {

  @Autowired
  lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var aliasNameGenerator: AliasNameGenerator

  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    private const val DEV_USER_PASSWORD = "123"
    private const val DEV_USER_PASSWORD_HASH = "{noop}$DEV_USER_PASSWORD"
    val admin = User("admin").apply {
      roles = mutableSetOf(Role.ADMIN)
      firstName = "John"
      lastName = "Admin"
      password = DEV_USER_PASSWORD_HASH
    }
    val teacher = User("teacher").apply {
      roles = mutableSetOf(Role.TEACHER)
      firstName = "Kim"
      lastName = "Teacher"
      password = DEV_USER_PASSWORD_HASH
    }
    val student = User("student").apply {
      roles = mutableSetOf(Role.STUDENT)
      firstName = "Alice"
      lastName = "Student"
      password = DEV_USER_PASSWORD_HASH
    }

    /**
     * Added couple more users for testing
     */
    val student2 = User("student2").apply {
      roles = mutableSetOf(Role.STUDENT)
      firstName = "Karl"
      lastName = "Student"
      password = DEV_USER_PASSWORD_HASH
    }
    val student3 = User("student3").apply {
      roles = mutableSetOf(Role.STUDENT)
      firstName = "Max"
      lastName = "Student"
      password = DEV_USER_PASSWORD_HASH
    }
    val student4 = User("student4").apply {
      roles = mutableSetOf(Role.STUDENT)
      firstName = "Anna"
      lastName = "Student"
      password = DEV_USER_PASSWORD_HASH
    }
    val student5 = User("student5").apply {
      roles = mutableSetOf(Role.STUDENT)
      firstName = "Eve"
      lastName = "Student"
      password = DEV_USER_PASSWORD_HASH
    }
    val student6 = User("student6").apply {
      roles = mutableSetOf(Role.STUDENT)
      firstName = "Klaus"
      lastName = "Student"
      password = DEV_USER_PASSWORD_HASH
    }
    val student7 = User("student7").apply {
      roles = mutableSetOf(Role.STUDENT)
      firstName = "Hannes"
      lastName = "Student"
      password = DEV_USER_PASSWORD_HASH
    }
    val student8 = User("student8").apply {
      roles = mutableSetOf(Role.STUDENT)
      firstName = "Richard"
      lastName = "Student"
      password = DEV_USER_PASSWORD_HASH
    }
    val student9 = User("student9").apply {
      roles = mutableSetOf(Role.STUDENT)
      firstName = "Lena"
      lastName = "Student"
      password = DEV_USER_PASSWORD_HASH
    }
    val student10 = User("student10").apply {
      roles = mutableSetOf(Role.STUDENT)
      firstName = "Emily"
      lastName = "Student"
      password = DEV_USER_PASSWORD_HASH
    }

    val allUsers = listOf(admin, teacher, student,student2,student3,student4,student5,student6,student7,student8,student9,student10)
  }

  override fun onApplicationEvent(event: ContextRefreshedEvent) {
    if (userRepository.count() > 0) {
      return log.info("Not adding dummy users to database as it already contains users.")
    }
    log.info(
        "Adding some dummy users to database with password '$DEV_USER_PASSWORD': " +
            allUsers.joinToString { it.username }
    )

    val users = userRepository.saveAll(allUsers)
    users.forEach{
      aliasNameGenerator.applyAliasName(it)
    }
  }
}
