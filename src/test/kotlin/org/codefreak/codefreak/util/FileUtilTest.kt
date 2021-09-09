package org.codefreak.codefreak.util

import java.io.File
import org.junit.Assert
import org.junit.Test

class FileUtilTest {
  @Test
  fun `file name is normalized correctly`() {
    mapOf(
      "foo" to "foo",
      // keep leading/trailing slashes as they are valid in filenames
      " foo " to " foo ",
      // resulting path will contain the correct directory separator
      "foo/bar" to "foo" + File.separatorChar + "bar",
      ".foo" to ".foo",
      ".foo.bar" to ".foo.bar",
      ".foo/bar" to ".foo" + File.separatorChar + "bar",
      ".foo/.bar" to ".foo" + File.separatorChar + ".bar",
      "./" to "",
      "." to "",
      "../" to "",
      "../." to "",
      "../.." to "",
      ".././foo" to "foo",
      ".././foo//../bar" to "bar"
    ).forEach {
      Assert.assertEquals(it.value, FileUtil.sanitizePath(it.key))
    }
  }
}
