package de.code_freak.codefreak.auth

import de.code_freak.codefreak.util.FrontendUtil
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component

@Aspect
@Component
class AllowRolesSecurityAspect {

  @Pointcut("@annotation(de.code_freak.codefreak.auth.AllowRoles)")
  private fun allowRolesAnnotation() {}

  @Around("de.code_freak.codefreak.auth.AllowRolesSecurityAspect.allowRolesAnnotation()")
  fun checkRoles(joinPoint: ProceedingJoinPoint): Any {

    val roles = (joinPoint.signature as MethodSignature).method.getAnnotation(AllowRoles::class.java).value
        .map { it.authority }

    if (FrontendUtil.getUser().authorities.firstOrNull { roles.contains(it.authority) } == null) {
      throw AccessDeniedException("Access Denied")
    }
    return joinPoint.proceed()
  }
}
