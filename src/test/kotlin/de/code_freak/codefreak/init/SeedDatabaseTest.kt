package de.code_freak.codefreak.init

import de.code_freak.codefreak.repository.AssignmentRepository
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(locations = ["/application-test.properties"])
internal class SeedDatabaseTest {

  @Autowired
  private lateinit var assignmentRepository: AssignmentRepository

  @Test
  fun `seed database is executed in dev profile`() {
    assertTrue(assignmentRepository.findAll().count() > 0)
  }
}
