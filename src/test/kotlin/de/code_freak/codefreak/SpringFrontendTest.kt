package de.code_freak.codefreak

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc

@AutoConfigureMockMvc
abstract class SpringFrontendTest : SpringTest() {
  @Autowired
  protected lateinit var mockMvc: MockMvc
}
