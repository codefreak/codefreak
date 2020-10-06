package org.codefreak.codefreak.entity

import javax.persistence.PostPersist
import javax.persistence.PostUpdate
import javax.persistence.PreRemove
import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions
import org.springframework.context.ApplicationContext
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EntityListener(vararg val value: KClass<*> = [])

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class SpringEntityListenerAdapter(ctx: ApplicationContext) {

  private val listenerBeans = ctx.getBeansWithAnnotation(EntityListener::class.java).values.toList()

  @PostUpdate
  fun postUpdate(entity: Any) = delegateToBeans(entity, PostUpdate::class)

  @PostPersist
  fun postPersist(entity: Any) = delegateToBeans(entity, PostPersist::class)

  @PreRemove
  fun preRemove(entity: Any) = delegateToBeans(entity, PreRemove::class)

  private fun delegateToBeans(entity: Any, annotation: KClass<*>) {
    listenerBeans
        .filter { isListenerForEntityType(it, entity) }
        .forEach { listener ->
          listener::class.memberFunctions
              .filter { it.annotations.filterIsInstance(annotation.java).isNotEmpty() }
              .forEach { it.call(listener, entity) }
        }
  }

  private fun isListenerForEntityType(listener: Any, entity: Any): Boolean {
    listener::class.annotations.forEach {
      if (it is EntityListener && it.value.contains(entity::class)) {
        return true
      }
    }
    return false
  }
}
