package org.codefreak.codefreak.init

import org.codefreak.codefreak.SpringTest
import org.codefreak.codefreak.repository.AssignmentRepository
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@ActiveProfiles("dev")
@TestPropertySource(locations = ["/application-test.yml"])
internal class SeedDatabaseTest : SpringTest() {

  @Autowired
  private lateinit var assignmentRepository: AssignmentRepository

  @Test
  fun `seed database is executed in dev profile`() {
    assertTrue(assignmentRepository.findAll().count() > 0)
  }
}
