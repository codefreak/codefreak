package org.codefreak.codefreak.service.workspace

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.io.ByteArrayOutputStream
import java.util.UUID
import org.codefreak.codefreak.EXTERNAL_INTEGRATION_TEST
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.service.WorkspaceBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired

private val ANSWER_ID = UUID(0, 0)

@Tag(EXTERNAL_INTEGRATION_TEST)
class WorkspaceIdeServiceTest : WorkspaceBaseTest() {

  @Autowired
  private lateinit var workspaceIdeService: WorkspaceIdeService

  @Mock
  private lateinit var answer: Answer

  @BeforeEach
  fun beforeEach() {
    val tar = createTarWithEntries(mapOf("file.txt" to "foo"))
    whenever(fileService.readCollectionTar(any())).thenReturn(tar)
    whenever(fileService.writeCollectionTar(any())).thenReturn(ByteArrayOutputStream())
    whenever(answer.id).thenReturn(ANSWER_ID)
  }

  @Test
  @Order(1)
  fun createAnswerIde() {
    workspaceIdeService.createAnswerIde(answer)
    verify(fileService, times(1)).readCollectionTar(ANSWER_ID)
  }

  @Test
  @Order(2)
  fun saveAnswerFiles() {
    workspaceIdeService.saveAnswerFiles(ANSWER_ID)
    verify(fileService, times(1)).writeCollectionTar(ANSWER_ID)
  }

  @Test
  @Order(3)
  fun redeployAnswerFiles() {
    workspaceIdeService.redeployAnswerFiles(ANSWER_ID)
    verify(fileService, times(1)).readCollectionTar(ANSWER_ID)
  }

  @Test
  @Order(4)
  fun deleteAnswerIde() {
    workspaceIdeService.deleteAnswerIde(ANSWER_ID)
    verify(fileService, times(1)).writeCollectionTar(ANSWER_ID)
  }
}
