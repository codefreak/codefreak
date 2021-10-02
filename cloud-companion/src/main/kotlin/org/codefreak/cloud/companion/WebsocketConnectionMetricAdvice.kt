package org.codefreak.cloud.companion

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.BaseUnits
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Advice that counts the number of open websocket connections by wrapping around the {WebSocketHandler#handle} method.
 * The counter is increased every time the returned Mono is subscribed to and decreased when it is unsubscribed.
 */
@Aspect
@Component
class WebsocketConnectionMetricAdvice(
  meterRegistry: MeterRegistry
) {
    companion object {
        private val log = LoggerFactory.getLogger(WebsocketConnectionMetricAdvice::class.java)
    }

    private val connectionCounter = AtomicInteger()

    init {
        Gauge.builder("http.websocket.connections", connectionCounter) { it.get().toDouble() }
            .description("Number of active websocket connections")
            .baseUnit(BaseUnits.CONNECTIONS)
            .register(meterRegistry)
    }

    @Around(value = "execution(* org.springframework.web.reactive.socket.WebSocketHandler.handle(..))")
    fun onHandle(pjp: ProceedingJoinPoint): Mono<*> {
        val producer = pjp.proceed(pjp.args) as Mono<*>
        return producer.doOnSubscribe {
            registerNewConnection()
        }.doFinally {
            unregisterConnection()
        }
    }

    private fun registerNewConnection() {
        val newCount = connectionCounter.incrementAndGet()
        log.debug("Client connected! New connection count: $newCount")
    }

    private fun unregisterConnection() {
        var newCount: Int
        do {
            val currentCount = connectionCounter.get()
            newCount = max(0, currentCount - 1)
        } while (!connectionCounter.compareAndSet(currentCount, newCount))
        log.debug("Client disconnected! New connection count: $newCount")
    }
}
