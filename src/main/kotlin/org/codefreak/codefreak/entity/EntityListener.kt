package org.codefreak.codefreak.entity

import javax.persistence.PostPersist
import javax.persistence.PostUpdate
import javax.persistence.PreRemove
import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaMethod
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EntityListener(vararg val value: KClass<*> = [])

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class SpringEntityListenerAdapter : ApplicationContextAware {

  private lateinit var listenerBeans: List<Any>

  override fun setApplicationContext(applicationContext: ApplicationContext) {
    this.listenerBeans = applicationContext.getBeansWithAnnotation(EntityListener::class.java).values.toList()
  }

  @PostUpdate
  fun postUpdate(entity: Any) = delegateToBeans(entity, PostUpdate::class)

  @PostPersist
  fun postPersist(entity: Any) = delegateToBeans(entity, PostPersist::class)

  @PreRemove
  fun preRemove(entity: Any) = delegateToBeans(entity, PreRemove::class)

  private fun delegateToBeans(entity: Any, annotation: KClass<out Annotation>) {
    listenerBeans
        .filter { isListenerForEntityType(it, entity) }
        .forEach { listener ->
          listener::class.memberFunctions
              .filter {
                val javaMethod = it.javaMethod ?: return@filter false
                AnnotationUtils.findAnnotation(javaMethod, annotation.java) != null
              }
              .forEach { it.call(listener, entity) }
        }
  }

  private fun isListenerForEntityType(listener: Any, entity: Any): Boolean {
    val annotation = AnnotationUtils.findAnnotation(listener::class.java, EntityListener::class.java) ?: return false
    return annotation.value.contains(entity::class)
  }
}
