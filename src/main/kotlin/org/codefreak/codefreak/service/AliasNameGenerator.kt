package org.codefreak.codefreak.service

import kotlin.random.Random
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.entity.UserAlias
import org.codefreak.codefreak.repository.UserRepository
import org.codefreak.codefreak.service.evaluation.GradeDefinitionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * This Class imports a set of Names and contains functions to add those names as Aliases to existing users.
 * Main purpose of this class is to give students some kind of anonymity if they results are shown on a scoreboard
 */
@Component
class AliasNameGenerator {

  /**
   * Logging
   */
  val log = LoggerFactory.getLogger(GradeDefinitionService::class.simpleName)!!

  @Autowired
  private lateinit var userRepository: UserRepository
  @Autowired
  private lateinit var userAliasService: UserAliasService

  /**
   * Init function to give all users an alias name.
   * just uncomment to perform on startup
   */
//  @PostConstruct
  fun initialize() {
    val userList = userRepository.findAll()
    for (user in userList) {
      if (user.userAlias == null) {
        applyAliasName(user)
      }
    }
  }

  /**
   * Gets the Inputstream of an animals file and mutate it to an string array.
   */
  private fun getAnimalSchema() = ClassPathResource("evaluation/animals.json").inputStream.use { String(it.readBytes()) }

  /**
   * Imports all animals from a given File and generates  List of animals.
   */
  private fun importAnimalNamesFromJson(): MutableList<Animal> {
    val reg = Regex("[^A-Za-z0-9]")

    val animals = mutableListOf<Animal>()
    val input = getAnimalSchema()
    val inputList = input.lines()
    inputList.forEach {
      val name = it.replace(reg, "")
      if (name.isNotEmpty()) {
        animals.add(Animal(name))
      }
    }
    return animals
  }

  /**
   * returns a random animal
   */
  private fun getRandomAnimal(): Animal {
    val animals = importAnimalNamesFromJson()
    val rng = rand(animals.size)
    return animals[rng]
  }

  /**
   * Applies an Alias name to an user
   */
  fun applyAliasName(user: User) {
    // Get Random Animal Name
    val animalName = getRandomAnimal().name
    val seedNumber = rand(9999)
    // Build name - looks nice
    val fullAliasName = "$animalName#$seedNumber"
    // Create and Build on User
    if (!userAliasService.existsByAlias(fullAliasName)) {
      user.userAlias = UserAlias(user, fullAliasName)
    } else {
      // Recursion
      applyAliasName(user)
    }
    log.info("aliasName set to $fullAliasName")
    userRepository.save(user)
  }

  /**
   * returns a random number between start and end.
   */
  private fun rand(end: Int): Int {
    return Random(System.nanoTime()).nextInt(0, end)
  }

  /**
   * Simple Class of Animal.
   */
  class Animal(
    var name: String
  )
}
