package org.codefreak.codefreak.frontend

import java.util.UUID
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.auth.Authorization
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@Controller
@RequestMapping("/api/answers")
class AnswerController : BaseController() {

  @Autowired
  lateinit var fileService: FileService

  @GetMapping("/{answerId}/source.zip", produces = ["application/zip"])
  @ResponseBody
  fun getSourceZip(@PathVariable("answerId") answerId: UUID): HttpEntity<StreamingResponseBody> {
    val answer = answerService.findAnswer(answerId)
    Authorization().requireAuthorityIfNotCurrentUser(answer.submission.user, Authority.ROLE_TEACHER)
    fileService.readCollectionTar(answer.id).use {
      return download("${answer.submission.user.username}_${answer.task.title}.zip") { out ->
        TarUtil.tarToZip(it, out)
      }
    }
  }

  @GetMapping("/{answerId}/source.tar", produces = ["application/tar"])
  @ResponseBody
  fun getSourceTar(@PathVariable("answerId") answerId: UUID): HttpEntity<StreamingResponseBody> {
    val answer = answerService.findAnswer(answerId)
    Authorization().requireAuthorityIfNotCurrentUser(answer.submission.user, Authority.ROLE_TEACHER)
    fileService.readCollectionTar(answer.id).use {
      return download("${answer.submission.user.username}_${answer.task.title}.tar", it)
    }
  }
}
