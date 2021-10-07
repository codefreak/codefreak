package org.codefreak.codefreak.util

import java.io.InputStream
import java.io.OutputStream
import java.util.Optional
import java.util.UUID
import org.apache.commons.io.input.ProxyInputStream
import org.apache.commons.io.output.ProxyOutputStream
import org.codefreak.codefreak.config.EvaluationConfiguration
import org.slf4j.Logger
import org.springframework.batch.core.JobParameters

fun String.withoutTrailingSlash(): String = trimEnd('/')
fun String.withoutLeadingSlash(): String = trimStart('/')
fun String.withTrailingSlash(): String = if (endsWith("/")) this else "$this/"
fun String.withLeadingSlash(): String = if (startsWith("/")) this else "/$this"

fun OutputStream.afterClose(callback: () -> Any?) = object : ProxyOutputStream(this) {
  override fun close() {
    try {
      super.close()
    } finally {
      callback()
    }
  }
}

fun InputStream.preventClose() = object : ProxyInputStream(this) {
  override fun close() {
    // nope
  }
}

fun Logger.error(e: Throwable) {
  error(e.message)
  e.printStackTrace()
}

fun <T> Optional<T>.orNull(): T? = orElse(null)

/**
 * Requires when() to be exhaustive at compile time
 */
val <T> T.exhaustive: T
  get() = this

val JobParameters.evaluationStepId: UUID?
  get() = getString(EvaluationConfiguration.PARAM_EVALUATION_STEP_ID)?.let { id -> UUID.fromString(id) }

fun String.wrapInMarkdownCodeBlock() = if (isNotBlank()) "```\n$this\n```" else ""
