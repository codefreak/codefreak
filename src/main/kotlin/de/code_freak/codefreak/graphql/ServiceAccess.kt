package de.code_freak.codefreak.graphql

import de.code_freak.codefreak.service.BaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class ServiceAccess {

  @Autowired
  private lateinit var applicationContext: ApplicationContext

  fun <T : BaseService> getService(type: KClass<T>): T = applicationContext.getBean(type.java)
}
