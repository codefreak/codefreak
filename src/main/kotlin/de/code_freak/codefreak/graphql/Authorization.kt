package de.code_freak.codefreak.graphql

import de.code_freak.codefreak.auth.Authorization
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.service.SessionService
import graphql.schema.DataFetchingEnvironment
import graphql.servlet.context.DefaultGraphQLWebSocketContext

internal const val SESSION_COOKIE = "JSESSIONID="

fun <T> authorized(env: DataFetchingEnvironment, serviceAccess: ServiceAccess, block: Authorization.() -> T): T {
  val sessionId = when (val context = env.getContext<Any>()) {
    is DefaultGraphQLWebSocketContext -> context.handshakeRequest.headers["cookie"]
        ?.first { it.startsWith(SESSION_COOKIE) }
        ?.drop(SESSION_COOKIE.length)
    else -> throw UnsupportedOperationException()
  }
  val session = sessionId?.let { serviceAccess.getService(SessionService::class).getSession(it) } ?: Authorization.deny()
  return Authorization(session.principal as User).block()
}
