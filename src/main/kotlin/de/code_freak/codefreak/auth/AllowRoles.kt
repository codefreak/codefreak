package de.code_freak.codefreak.auth

/**
 * Use this on a controller method. The authenticated user needs any of the listed roles to be granted access.
 *
 * @see AllowRolesSecurityAspect
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AllowRoles(vararg val value: Role)
