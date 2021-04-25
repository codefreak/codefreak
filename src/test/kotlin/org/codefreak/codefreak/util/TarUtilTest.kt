package org.codefreak.codefreak.util

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.notNullValue
import org.junit.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.mock.web.MockPart

internal class TarUtilTest {

  @Test
  fun `tar is created correctly`() {
    val out = ByteArrayOutputStream()
    TarUtil.createTarFromDirectory(ClassPathResource("util/tar-sample").file, out)
    TarArchiveInputStream(out.toByteArray().inputStream()).use {
      val result = generateSequence { it.nextTarEntry }.map { it.name }.toList()
      assertThat(result, containsInAnyOrder("/", "executable.sh", "foo.txt", "subdir/", "subdir/bar.txt"))
    }
  }

  @Test
  fun `tar with long file names is created correctly`() {
    val out = ByteArrayOutputStream()
    val veryLongName = "a".repeat(101)
    val tmpDir = Files.createTempDirectory("very-long-test-").toFile()
    File(tmpDir, veryLongName).createNewFile()
    TarUtil.createTarFromDirectory(tmpDir, out)
    tmpDir.deleteRecursively()
    TarArchiveInputStream(out.toByteArray().inputStream()).use {
      val result = generateSequence { it.nextTarEntry }.map { it.name }.toList()
      assertThat(result, hasItem(veryLongName))
    }
  }

  @Test
  fun `tar persists execute permissions`() {
    val out = ByteArrayOutputStream()
    TarUtil.createTarFromDirectory(ClassPathResource("util/tar-sample").file, out)
    TarArchiveInputStream(out.toByteArray().inputStream()).use {
      val result = generateSequence { it.nextTarEntry }.filter { it.name == "executable.sh" }.first()
      // octal 100744 = int 33252
      assertThat(result.mode, `is`(33252))
    }
  }

  @Test
  fun `file uploads are wrapped in tar`() {
    val file = MockPart("file", "C:\\Users\\jdoe\\main.c", "".toByteArray())
    val out = ByteArrayOutputStream()
    TarUtil.writeUploadAsTar(arrayOf(file), out)
    TarArchiveInputStream(out.toByteArray().inputStream()).use {
      val result = generateSequence { it.nextTarEntry }.filter { it.name == "main.c" }.firstOrNull()
      assertThat(result, notNullValue())
    }
  }
}
