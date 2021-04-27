package org.codefreak.codefreak.auth

import org.codefreak.codefreak.SpringFrontendTest
import org.codefreak.codefreak.frontend.BaseController
import org.codefreak.codefreak.service.SeedDummyUsersService
import org.codefreak.codefreak.service.UserService
import org.junit.Test
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Component
class TestUserDetailsService(private val userService: UserService) : UserDetailsService {
  override fun loadUserByUsername(username: String): UserDetails = when (username) {
    "admin" -> SeedDummyUsersService.admin
    "teacher" -> SeedDummyUsersService.teacher
    "student" -> SeedDummyUsersService.student
    "student2" -> SeedDummyUsersService.student2
    "student3" -> SeedDummyUsersService.student3
    "student4" -> SeedDummyUsersService.student4
    "student5" -> SeedDummyUsersService.student5
    else -> throw UsernameNotFoundException("User $username cannot be found")
  }
}

@Controller
@ResponseBody
@RequestMapping("/allowRolesTest")
class AuthorizationTestController : BaseController() {

  @Secured(Authority.ROLE_ADMIN)
  @GetMapping("/allowAdmin")
  fun allowAdmin(): String {
    return "OK"
  }

  @Secured(Authority.ROLE_TEACHER)
  @GetMapping("/allowTeacher")
  fun allowTeacher(): String {
    return "OK"
  }

  @GetMapping("/allowAll")
  fun allowAll(): String {
    return "OK"
  }
}

class AuthorizationTest : SpringFrontendTest() {

  @Test
  @WithUserDetails(value = "admin", userDetailsServiceBeanName = "testUserDetailsService")
  fun requestWithAdminUser() {
    mockMvc.perform(get("/allowRolesTest/allowAdmin")).andExpect(status().isOk)
    mockMvc.perform(get("/allowRolesTest/allowTeacher")).andExpect(status().isOk)
    mockMvc.perform(get("/allowRolesTest/allowAll")).andExpect(status().isOk)
  }

  @Test
  @WithUserDetails(value = "teacher", userDetailsServiceBeanName = "testUserDetailsService")
  fun requestWithTeacherUser() {
    mockMvc.perform(get("/allowRolesTest/allowAdmin")).andExpect(status().isForbidden)
    mockMvc.perform(get("/allowRolesTest/allowTeacher")).andExpect(status().isOk)
    mockMvc.perform(get("/allowRolesTest/allowAll")).andExpect(status().isOk)
  }

  @Test
  @WithUserDetails(value = "student", userDetailsServiceBeanName = "testUserDetailsService")
  fun requestWithStudentUser() {
    mockMvc.perform(get("/allowRolesTest/allowAdmin")).andExpect(status().isForbidden)
    mockMvc.perform(get("/allowRolesTest/allowTeacher")).andExpect(status().isForbidden)
    mockMvc.perform(get("/allowRolesTest/allowAll")).andExpect(status().isOk)
  }
}
