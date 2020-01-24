package de.code_freak.codefreak.graphql

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.util.ReflectionUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.function.Consumer
import javax.annotation.PostConstruct

// Based on https://developer.okta.com/blog/2018/09/24/reactive-apis-with-spring-webflux

open class SubscriptionEventPublisher<T : ApplicationEvent> : ApplicationListener<T>, Consumer<FluxSink<T>> {

  private val executor = Executors.newSingleThreadExecutor()
  private val queue = LinkedBlockingQueue<T>()
  val eventStream by lazy { Flux.create(this).share() }

  @PostConstruct
  private fun init() {
    // We create an initial subscriber that discards all events, so that
    // 1. the first real subscriber won't get the backlog of past events
    // 2. the flux is not closed when there are no current subscribers
    eventStream.subscribe()
  }

  override fun onApplicationEvent(event: T) {
    queue.offer(event)
  }

  override fun accept(sink: FluxSink<T>) {
    executor.execute {
      while (true) {
        try {
          sink.next(queue.take())
        } catch (e: InterruptedException) {
          ReflectionUtils.rethrowException(e)
        }
      }
    }
  }
}
