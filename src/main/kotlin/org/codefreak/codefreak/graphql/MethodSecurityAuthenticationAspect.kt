package org.codefreak.codefreak.graphql

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.codefreak.codefreak.util.FrontendUtil
import org.springframework.core.annotation.Order
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import org.codefreak.codefreak.auth.NotAuthenticatedException

/**
 * This aspect is executed for all GraphQL resolver methods that use method security.
 * By default, Spring Security will throw an [AccessDeniedException] if the user is not
 * authenticated at all or the session has expired. We want to distinguish this from
 * missing permissions and throw a [NotAuthenticatedException] instead.
 */
@Aspect
@Component
@Order(1)
class MethodSecurityAuthenticationAspect {

  @Pointcut("execution(* org.codefreak.codefreak.graphql.BaseResolver+.*(..))")
  fun resolverMethod() {}

  @Pointcut("@annotation(org.springframework.security.access.annotation.Secured)")
  fun securedAnnotation() {}

  @Pointcut("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
  fun preAuthorizeAnnotation() {}

  @Around("resolverMethod() && (securedAnnotation() || preAuthorizeAnnotation())")
  fun checkAuthentication(joinPoint: ProceedingJoinPoint): Any? {
    FrontendUtil.getCurrentUser() // throws if not authenticated
    return joinPoint.proceed()
  }
}
