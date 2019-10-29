package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import de.code_freak.codefreak.util.FrontendUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
import org.springframework.stereotype.Component

@GraphQLName("Authentication")
class AuthenticationDto(val token: String, val user: UserDto)

@Component
class AuthMutation : Mutation {

  @Autowired(required = false)
  private lateinit var authenticationManager: AuthenticationManager

  fun login(username: String, password: String): AuthenticationDto {
    val auth = authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
    val securityContext = SecurityContextHolder.getContext()
    securityContext.authentication = auth
    val session = FrontendUtil.getRequest().getSession(true)
    session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext)

    return AuthenticationDto(session.id, UserDto(FrontendUtil.getCurrentUser().entity))
  }
}
