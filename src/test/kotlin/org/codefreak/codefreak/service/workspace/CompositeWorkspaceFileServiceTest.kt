package org.codefreak.codefreak.service.workspace

import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.function.Consumer
import java.util.function.Supplier
import org.codefreak.codefreak.service.workspace.WorkspacePurpose.ANSWER_IDE
import org.codefreak.codefreak.service.workspace.WorkspacePurpose.EVALUATION
import org.codefreak.codefreak.service.workspace.WorkspacePurpose.TASK_IDE
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class CompositeWorkspaceFileServiceTest {
  private val emptyInputStream = ByteArrayInputStream(ByteArray(0))

  @Test
  fun `save is delegated correctly`() {
    val fs1 = mock(CompositeWorkspaceFileService.ScopedFileWorkspaceService::class.java)
    whenever(fs1.supportedPurposes).thenReturn(setOf(ANSWER_IDE, TASK_IDE))
    val fs2 = mock(CompositeWorkspaceFileService.ScopedFileWorkspaceService::class.java)
    whenever(fs2.supportedPurposes).thenReturn(setOf(EVALUATION))

    val id1 = WorkspaceIdentifier(ANSWER_IDE, "foo1")
    val supplier1 = Supplier<InputStream> { emptyInputStream }
    val id2 = WorkspaceIdentifier(TASK_IDE, "foo2")
    val supplier2 = Supplier<InputStream> { emptyInputStream }
    val id3 = WorkspaceIdentifier(EVALUATION, "foo3")
    val supplier3 = Supplier<InputStream> { emptyInputStream }

    val compositeWorkspaceFileService = CompositeWorkspaceFileService(listOf(fs1, fs2))
    compositeWorkspaceFileService.saveFiles(id1, supplier1)
    compositeWorkspaceFileService.saveFiles(id2, supplier2)
    compositeWorkspaceFileService.saveFiles(id3, supplier3)

    verify(fs1, times(1)).saveFiles(id1, supplier1)
    verify(fs1, times(1)).saveFiles(id2, supplier2)
    verify(fs2, times(1)).saveFiles(id3, supplier3)
  }

  @Test
  fun `load is delegated correctly`() {
    val fs1 = mock(CompositeWorkspaceFileService.ScopedFileWorkspaceService::class.java)
    whenever(fs1.supportedPurposes).thenReturn(setOf(ANSWER_IDE, TASK_IDE))
    val fs2 = mock(CompositeWorkspaceFileService.ScopedFileWorkspaceService::class.java)
    whenever(fs2.supportedPurposes).thenReturn(setOf(EVALUATION))
    val id1 = WorkspaceIdentifier(ANSWER_IDE, "foo1")
    val consumer1 = Consumer<InputStream> { assertSame(it, emptyInputStream) }
    val id2 = WorkspaceIdentifier(TASK_IDE, "foo2")
    val consumer2 = Consumer<InputStream> { assertSame(it, emptyInputStream) }
    val id3 = WorkspaceIdentifier(EVALUATION, "foo3")
    val consumer3 = Consumer<InputStream> { assertSame(it, emptyInputStream) }

    val compositeWorkspaceFileService = CompositeWorkspaceFileService(listOf(fs1, fs2))
    compositeWorkspaceFileService.loadFiles(id1, consumer1)
    compositeWorkspaceFileService.loadFiles(id2, consumer2)
    compositeWorkspaceFileService.loadFiles(id3, consumer3)

    verify(fs1, times(1)).loadFiles(id1, consumer1)
    verify(fs1, times(1)).loadFiles(id2, consumer2)
    verify(fs2, times(1)).loadFiles(id3, consumer3)
  }

  @Test
  fun `throws when no delegate is available`() {
    val compositeWorkspaceFileService = CompositeWorkspaceFileService(emptyList())
    assertThrows(IllegalArgumentException::class.java) {
      compositeWorkspaceFileService.saveFiles(WorkspaceIdentifier(ANSWER_IDE, "foo1")) { emptyInputStream }
    }

    assertThrows(IllegalArgumentException::class.java) {
      compositeWorkspaceFileService.loadFiles(WorkspaceIdentifier(ANSWER_IDE, "foo1")) { /* no-op */ }
    }
  }
}
