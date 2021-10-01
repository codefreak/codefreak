package org.codefreak.cloud.companion

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.aspectj.lang.ProceedingJoinPoint
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

internal class WebsocketConnectionMetricAdviceTest {

    private lateinit var connectionMetricAdvice: WebsocketConnectionMetricAdvice
    private lateinit var gauge: Gauge

    @BeforeEach
    fun beforeEach() {
        val meterRegistry = SimpleMeterRegistry()
        connectionMetricAdvice = WebsocketConnectionMetricAdvice(meterRegistry)
        gauge = meterRegistry.get("http.websocket.connections").gauge()
    }

    @Test
    fun testCountsUpAndDown() {
        val pjp = Mockito.mock(ProceedingJoinPoint::class.java)
        `when`(pjp.proceed(any())).thenReturn(Mono.just("1"))
        val wrappedMono = connectionMetricAdvice.onHandle(pjp)

        assertThat(gauge.value(), `is`(.0))
        StepVerifier.create(wrappedMono)
            .assertNext { assertThat(gauge.value(), `is`(1.0)) }
            .verifyComplete()
        assertThat(gauge.value(), `is`(.0))
    }
}
