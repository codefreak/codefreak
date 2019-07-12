package de.code_freak.codefreak.auth

import de.code_freak.codefreak.SpringFrontendTest
import de.code_freak.codefreak.frontend.BaseController
import org.junit.Test
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.stereotype.Controller
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@ResponseBody
@RequestMapping("/allowRolesTest")
class AllowRolesTestController : BaseController() {

  @AllowRoles(Role.ADMIN)
  @GetMapping("/allowAdmin")
  fun allowAdmin(): String {
    return "OK"
  }

  @AllowRoles(Role.TEACHER)
  @GetMapping("/allowTeacher")
  fun allowTeacher(): String {
    return "OK"
  }

  @GetMapping("/allowAll")
  fun allowAll(): String {
    return "OK"
  }
}

class AllowRolesTest : SpringFrontendTest() {

  @Test
  @WithUserDetails("admin")
  fun requestWithAdminUser() {
    mockMvc.perform(get("/allowRolesTest/allowAdmin")).andExpect(status().isOk)
    mockMvc.perform(get("/allowRolesTest/allowTeacher")).andExpect(status().isOk)
    mockMvc.perform(get("/allowRolesTest/allowAll")).andExpect(status().isOk)
  }

  @Test
  @WithUserDetails("teacher")
  fun requestWithTeacherUser() {
    mockMvc.perform(get("/allowRolesTest/allowAdmin")).andExpect(status().isForbidden)
    mockMvc.perform(get("/allowRolesTest/allowTeacher")).andExpect(status().isOk)
    mockMvc.perform(get("/allowRolesTest/allowAll")).andExpect(status().isOk)
  }

  @Test
  @WithUserDetails("student")
  fun requestWithStudentUser() {
    mockMvc.perform(get("/allowRolesTest/allowAdmin")).andExpect(status().isForbidden)
    mockMvc.perform(get("/allowRolesTest/allowTeacher")).andExpect(status().isForbidden)
    mockMvc.perform(get("/allowRolesTest/allowAll")).andExpect(status().isOk)
  }
}
