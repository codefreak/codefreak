package org.codefreak.codefreak.graphql

import com.expediagroup.graphql.hooks.SchemaGeneratorHooks
import graphql.schema.GraphQLType
import org.springframework.stereotype.Component
import kotlin.reflect.KType

@Component
class SchemaGeneratorHooksImpl : SchemaGeneratorHooks {
  override fun willGenerateGraphQLType(type: KType): GraphQLType? {
    return type.classifier?.let { ScalarTypes.get(it) }
  }
}
