package org.codefreak.codefreak.graphql

import com.expediagroup.graphql.annotations.GraphQLIgnore

open class BaseDto(@GraphQLIgnore protected val ctx: ResolverContext) {
  @GraphQLIgnore
  protected val serviceAccess = ctx.serviceAccess

  @GraphQLIgnore
  protected val authorization = ctx.authorization
}
