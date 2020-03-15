package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.graphql.BaseResolver
import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.file.FileContentService
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.util.UUID

@GraphQLName("FileType")
enum class FileDtoType {
  FILE,
  DIRECTORY,
  SYMLINK
}

@GraphQLName("File")
class FileDto(val collectionId: UUID, val path: String, val content: String?, val type: FileDtoType) {
  constructor(collectionId: UUID, virtualFile: FileContentService.VirtualFile) : this(
      collectionId,
      path = virtualFile.path,
      // should use base64 encoding to transfer non-textual data
      // text is always treated as utf-8
      content = virtualFile.content?.toString(Charset.forName("UTF-8")),
      type = FileDtoType.valueOf(virtualFile.type.name)
  )
}

@Component
class FileQuery : BaseResolver(), Query {
  fun answerFiles(answerId: UUID): List<FileDto> = context {
    val answer = serviceAccess.getService(AnswerService::class).findAnswer(answerId)
    serviceAccess.getService(ContainerService::class).saveAnswerFiles(answer)
    authorization.requireAuthorityIfNotCurrentUser(answer.submission.user, Authority.ROLE_TEACHER)
    serviceAccess.getService(FileContentService::class).getFiles(answer.id).map {
      FileDto(answerId, it)
    }
  }

  fun answerFile(answerId: UUID, path: String): FileDto = context {
    val answer = serviceAccess.getService(AnswerService::class).findAnswer(answerId)
    authorization.requireAuthorityIfNotCurrentUser(answer.submission.user, Authority.ROLE_TEACHER)
    val file = serviceAccess.getService(FileContentService::class).getFile(answer.id, path)
    FileDto(answer.id, file)
  }
}