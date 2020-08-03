package org.codefreak.codefreak.graphql

import com.expediagroup.graphql.hooks.SchemaGeneratorHooks
import graphql.schema.GraphQLType
import kotlin.reflect.KType
import org.springframework.stereotype.Component

@Component
class SchemaGeneratorHooksImpl : SchemaGeneratorHooks {
  override fun willGenerateGraphQLType(type: KType): GraphQLType? {
    return type.classifier?.let { ScalarTypes.get(it) }
  }
}
