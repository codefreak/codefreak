package de.code_freak.codefreak.graphql

import de.code_freak.codefreak.auth.Authorization

open class ResolverContext(val serviceAccess: ServiceAccess, val authorization: Authorization)
