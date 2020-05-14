package org.codefreak.codefreak.util

import org.apache.commons.io.input.ProxyInputStream
import org.apache.commons.io.output.ProxyOutputStream
import org.slf4j.Logger
import java.io.InputStream
import java.io.OutputStream
import java.util.Optional

fun String.withoutTrailingSlash(): String = trimEnd('/')
fun String.withTrailingSlash(): String = if (endsWith("/")) this else "$this/"

fun OutputStream.afterClose(callback: () -> Any?) = object : ProxyOutputStream(this) {
  override fun close() {
    try {
      super.close()
    } finally {
      callback()
    }
  }
}

fun InputStream.afterClose(callback: () -> Any?) = object : ProxyInputStream(this) {
  override fun close() {
    try {
      super.close()
    } finally {
      callback()
    }
  }
}

fun Logger.error(e: Throwable) {
  error(e.message)
  e.printStackTrace()
}

fun <T> Optional<T>.orNull(): T? = orElse(null)
