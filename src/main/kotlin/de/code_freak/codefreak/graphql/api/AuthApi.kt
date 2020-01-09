package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.graphql.BaseDto
import de.code_freak.codefreak.graphql.BaseResolver
import de.code_freak.codefreak.graphql.ResolverContext
import de.code_freak.codefreak.service.SessionService
import de.code_freak.codefreak.util.FrontendUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
import org.springframework.stereotype.Component

@GraphQLName("Authentication")
class AuthenticationDto(val token: String, @GraphQLIgnore private val userEntity: User, ctx: ResolverContext) : BaseDto(ctx) {
  val user by lazy { UserDto(userEntity, ctx) }
}

@Component
class AuthMutation : BaseResolver(), Mutation {

  @Autowired(required = false)
  private lateinit var authenticationManager: AuthenticationManager

  fun login(username: String, password: String): AuthenticationDto {
    val auth = authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
    val securityContext = SecurityContextHolder.getContext()
    securityContext.authentication = auth
    val session = FrontendUtil.getRequest().getSession(true)
    session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext)
    return context {
      // we have to register the session ourselves because we do the login manually
      serviceAccess.getService(SessionService::class).registerNewSession(session.id, authorization.currentUser)
      AuthenticationDto(session.id, authorization.currentUser, this)
    }
  }

  fun logout(): Boolean {
    FrontendUtil.getRequest().getSession(false)?.invalidate()
    SecurityContextHolder.clearContext()
    return true
  }
}
