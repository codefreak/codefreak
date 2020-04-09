package org.codefreak.codefreak.graphql

import com.expediagroup.graphql.annotations.GraphQLIgnore
import org.codefreak.codefreak.auth.Authorization
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.service.SessionService
import graphql.schema.DataFetchingEnvironment
import graphql.servlet.context.GraphQLWebSocketContext
import org.springframework.beans.factory.annotation.Autowired

open class BaseResolver {

  companion object {
    private const val SESSION_COOKIE = "JSESSIONID="
  }

  @Autowired
  @GraphQLIgnore
  private lateinit var serviceAccess: ServiceAccess

  @GraphQLIgnore
  fun <T> context(block: ResolverContext.() -> T): T {
    return ResolverContext(serviceAccess, Authorization()).block()
  }

  @GraphQLIgnore
  fun <T> context(env: DataFetchingEnvironment, block: ResolverContext.() -> T): T {
    val ctx = env.getContext<Any>()
    val authorization = if (ctx is GraphQLWebSocketContext) {
      val sessionId = ctx.handshakeRequest.headers["cookie"]
          ?.flatMap { it.split(";") }
          ?.map { it.trim() }
          ?.firstOrNull { it.startsWith(SESSION_COOKIE) }
          ?.drop(SESSION_COOKIE.length)
      val session = sessionId?.let { serviceAccess.getService(SessionService::class).getSession(it) } ?: Authorization.deny()
      Authorization(session.principal as User)
    } else {
      Authorization()
    }
    return ResolverContext(serviceAccess, authorization).block()
  }
}
