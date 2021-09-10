package org.codefreak.codefreak.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FileUtilTest {
  @Test
  fun `file name is normalized correctly`() {
    mapOf(
      " " to "",
      "foo" to "foo",
      "foo " to "foo",
      "foo/bar" to "foo/bar",
      ".foo" to ".foo",
      ".foo.bar" to ".foo.bar",
      ".foo/bar" to ".foo/bar",
      ".foo/.bar" to ".foo/.bar",
      "./" to "",
      "." to "",
      "../" to "",
      "../." to "",
      "../.." to "",
      ".././foo" to "foo"
    ).forEach {
      assertEquals(it.value, FileUtil.sanitizeName(it.key))
    }
  }
}
