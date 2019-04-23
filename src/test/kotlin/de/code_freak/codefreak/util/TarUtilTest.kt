package de.code_freak.codefreak.util

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Test
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayInputStream

internal class TarUtilTest {

  @Test
  fun `tar is created correctly`() {
    val tar = TarUtil.createTarFromDirectory(ClassPathResource("util/tar-sample").file)
    TarArchiveInputStream(ByteArrayInputStream(tar)).use {
      val result = generateSequence { it.nextTarEntry }.map { it.name }.toList()
      assertThat(result, containsInAnyOrder("./", "./foo.txt", "./subdir/", "./subdir/bar.txt"))
    }
  }
}
