package org.codefreak.codefreak.graphql

import com.expediagroup.graphql.execution.FunctionDataFetcher
import com.expediagroup.graphql.execution.KotlinDataFetcherFactoryProvider
import com.expediagroup.graphql.hooks.SchemaGeneratorHooks
import graphql.schema.DataFetcherFactories
import graphql.schema.DataFetcherFactory
import kotlin.reflect.KFunction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Workaround for https://github.com/ExpediaGroup/graphql-kotlin/issues/518
 */
@Component
class DataFetcherFactoryProvider(@Autowired private val hooks: SchemaGeneratorHooks) : KotlinDataFetcherFactoryProvider(hooks) {

  private val shortCircuitObjectMapper = ShortCircuitObjectMapper()

  override fun functionDataFetcherFactory(target: Any?, kFunction: KFunction<*>): DataFetcherFactory<Any> =
      DataFetcherFactories.useDataFetcher(
          FunctionDataFetcher(
              target = target,
              fn = kFunction,
              objectMapper = shortCircuitObjectMapper,
              executionPredicate = hooks.dataFetcherExecutionPredicate))
}
