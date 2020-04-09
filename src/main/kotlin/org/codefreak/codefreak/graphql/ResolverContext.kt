package org.codefreak.codefreak.graphql

import org.codefreak.codefreak.auth.Authorization

open class ResolverContext(val serviceAccess: ServiceAccess, val authorization: Authorization)
