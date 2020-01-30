package de.code_freak.codefreak.graphql

import de.code_freak.codefreak.service.BaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * We use this for the GraphQL API to resolve dependencies at runtime. When the [SchemaPrinter] application is
 * run, the services are not in the application context.
 */
@Component
class ServiceAccess {

  @Autowired
  private lateinit var applicationContext: ApplicationContext

  fun <T : BaseService> getService(type: KClass<T>): T = applicationContext.getBean(type.java)
}
