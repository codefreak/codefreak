package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.graphql.BaseResolver
import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.file.FileContentService
import org.springframework.stereotype.Component
import java.util.UUID

@GraphQLName("File")
class FileDto(val collectionId: UUID, val path: String, val content: String)

@Component
class FileQuery : BaseResolver(), Query {
  fun answerFile(answerId: UUID, path: String): FileDto = context {
    val answer = serviceAccess.getService(AnswerService::class).findAnswer(answerId)
    authorization.requireAuthorityIfNotCurrentUser(answer.submission.user, Authority.ROLE_TEACHER)
    val file = serviceAccess.getService(FileContentService::class).getFile(answer.id, path)
    // should use base64 encoding to transfer non-textual data
    // text is always treated as utf-8
    FileDto(answer.id, file.path, String(file.content))
  }
}