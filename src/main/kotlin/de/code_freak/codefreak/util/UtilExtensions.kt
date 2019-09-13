package de.code_freak.codefreak.util

import org.apache.commons.io.input.ProxyInputStream
import org.apache.commons.io.output.ProxyOutputStream
import org.slf4j.Logger
import java.io.InputStream
import java.io.OutputStream

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
