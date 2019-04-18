package de.code_freak.codefreak.util

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayInputStream

internal class TarUtilTest {

  @Test
  fun `tar is created correctly`() {
    val result = TarUtil.createTarFromDirectory(ClassPathResource("util/tar-sample").file)
    TarArchiveInputStream(ByteArrayInputStream(result)).use { tar ->
      listOf("./", "./foo.txt", "./subdir/", "./subdir/bar.txt").forEach {
        assertEquals(it, tar.nextTarEntry.name)
      }
      assertEquals(null, tar.nextTarEntry)
    }
  }
}
