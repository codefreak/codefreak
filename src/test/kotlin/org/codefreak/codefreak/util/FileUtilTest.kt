package org.codefreak.codefreak.util

import java.io.File
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FileUtilTest {
  @Test
  fun `file name is normalized correctly`() {
    mapOf(
      "foo" to "foo",
      // keep leading/trailing spaces as they are valid in filenames
      " foo " to " foo ",
      "/foo" to "foo",
      "//foo" to "foo",
      "\\\\foo" to "foo",
      ".//foo" to "foo",
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
      assertEquals(it.value, FileUtil.normalizePath(it.key))
    }
  }

  @Test
  fun `that basename returns correct base`() {
    assertThat(FileUtil.basename("/foo/bar"), `is`("bar"))
    assertThat(FileUtil.basename("/foo/bar.txt"), `is`("bar.txt"))
    assertThat(FileUtil.basename("/foo/.gitignore"), `is`(".gitignore"))
    assertThat(FileUtil.basename("/foo/bar/"), `is`("bar"))
    assertThat(FileUtil.basename("/foo/ bar "), `is`(" bar "))
    assertThat(FileUtil.basename("/"), `is`(""))
    assertThat(FileUtil.basename("/ "), `is`(" "))
    assertThat(FileUtil.basename(""), `is`(""))
  }

  @Test
  fun `returns parent path`() {
    assertThat(FileUtil.getParentDir(""), `is`(File.separator))
    assertThat(FileUtil.getParentDir("foo/bar"), `is`("foo"))
    assertThat(FileUtil.getParentDir("foo"), `is`(File.separator))
    assertThat(FileUtil.getParentDir("/foo/bar"), `is`(File.separator + "foo"))
    assertThat(FileUtil.getParentDir("/foo"), `is`(File.separator))
  }

  @Test
  fun `returns parent paths`() {
    assertThat(FileUtil.getParentDirs("foo/bar/baz"), contains("foo" + File.separator + "bar", "foo"))
    assertThat(FileUtil.getParentDirs("foo/bar"), contains("foo"))
    assertThat(FileUtil.getParentDirs("foo"), empty())
    assertThat(
      FileUtil.getParentDirs("/foo/bar/baz"),
      contains(File.separator + "foo" + File.separator + "bar", File.separator + "foo")
    )
    assertThat(FileUtil.getParentDirs("/foo/bar"), contains(File.separator + "foo"))
    assertThat(FileUtil.getParentDirs("/foo"), empty())
  }
}
