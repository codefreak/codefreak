package org.codefreak.codefreak.service

import kotlin.random.Random
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * This Class imports a set of Names and contains functions to add those names as Aliases to existing users.
 * Main purpose of this class is to give students some kind of pseudonym if their results are shown on a scoreboard
 */
@Component
class AliasNameGenerator {

  @Autowired
  private lateinit var userRepository: UserRepository
  @Autowired
  private lateinit var userService: UserService

  /**
   * Gets the Inputstream of an animals file and mutate it to an string array.
   */
  private fun getAnimalSchema() = ClassPathResource("evaluation/animals.txt").inputStream.use { String(it.readBytes()) }

  /**
   * Imports all animals from a given File and generates  List of animals.
   *
   */
  private fun importAnimalNamesFromTextFile(): MutableList<Animal> {

    val animals = mutableListOf<Animal>()
    val input = getAnimalSchema()
    val inputList = input.lines()
    inputList.forEach {
      if (it.isNotEmpty()) {
        animals.add(Animal(it))
      }
    }
    return animals
  }

  private fun pickRandomAnimal(): Animal {
    val animals = importAnimalNamesFromTextFile()
    val rng = rand(animals.size)
    return animals[rng]
  }

  /**
   * Generates and Sets an Alias to a User.
   */
  fun generateAndSetAlias(user: User): User {
    if (user.alias != null) {
      return user
    } else {
      // get a random Animal Name and set it as alias
      do {
        val animalName = pickRandomAnimal().name
        // Seednumber required because an alias name is unique and there are more students than animals in the animals.txt
        val seedNumber = rand(9999)
        // Build name - looks nice
        val fullAliasName = "$animalName#$seedNumber"
        // Create and Build on User
        if (!userService.existsByAlias(fullAliasName)) {
          // alias for user is applied and saved.
          user.alias = fullAliasName
        }
      } while (user.alias == null)
      return userRepository.save(user)
    }
  }

  private fun rand(end: Int): Int {
    return Random(System.nanoTime()).nextInt(0, end)
  }

  class Animal(
    var name: String
  )
}
