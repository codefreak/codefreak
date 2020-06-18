package org.codefreak.codefreak.auth

import org.springframework.security.access.AccessDeniedException

/**
 * This is thrown to indicate that the user is either not authenticated at all or that their session has expired.
 */
class NotAuthenticatedException : AccessDeniedException("Not authenticated or session expired")
