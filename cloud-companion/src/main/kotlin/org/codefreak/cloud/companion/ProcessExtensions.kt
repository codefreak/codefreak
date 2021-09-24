package org.codefreak.cloud.companion

import java.io.InputStream
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

private fun fluxFromInputStream(inputStream: InputStream, factory: DataBufferFactory): Flux<DataBuffer> {
  return DataBufferUtils.readInputStream({ inputStream }, factory, 1024)
}

fun Process.getInputStreamFlux(factory: DataBufferFactory = DefaultDataBufferFactory.sharedInstance) =
  fluxFromInputStream(inputStream, factory)

fun Process.waitForMono(): Mono<Int> {
  return Mono.fromCallable {
    waitFor()
  }
}
