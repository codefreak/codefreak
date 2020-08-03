package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import java.nio.charset.Charset
import java.util.Base64
import java.util.UUID
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.auth.hasAuthority
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.ContainerService
import org.codefreak.codefreak.service.file.FileContentService
import org.codefreak.codefreak.service.file.FileService
import org.springframework.stereotype.Component

@GraphQLName("FileType")
enum class FileDtoType {
  FILE,
  DIRECTORY,
  SYMLINK
}

@GraphQLName("File")
class FileDto(
  val collectionId: UUID,
  val collectionDigest: String,
  val path: String,
  val content: String?,
  val type: FileDtoType
) {
  constructor(collectionId: UUID, collectionDigest: ByteArray, virtualFile: FileContentService.VirtualFile) : this(
      collectionId,
      Base64.getEncoder().encodeToString(collectionDigest),
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
    val forceSaveFiles = authorization.isCurrentUser(answer.task.owner) || authorization.currentUser.hasAuthority(Authority.ROLE_ADMIN)
    serviceAccess.getService(ContainerService::class).saveAnswerFiles(answer, forceSaveFiles)
    authorization.requireAuthorityIfNotCurrentUser(answer.submission.user, Authority.ROLE_TEACHER)
    val digest = serviceAccess.getService(FileService::class).getCollectionMd5Digest(answerId)
    serviceAccess.getService(FileContentService::class).getFiles(answer.id).map {
      FileDto(answerId, digest, it)
    }
  }

  fun answerFile(answerId: UUID, path: String): FileDto = context {
    val answer = serviceAccess.getService(AnswerService::class).findAnswer(answerId)
    authorization.requireAuthorityIfNotCurrentUser(answer.submission.user, Authority.ROLE_TEACHER)
    val file = serviceAccess.getService(FileContentService::class).getFile(answer.id, path)
    val digest = serviceAccess.getService(FileService::class).getCollectionMd5Digest(answerId)
    FileDto(answer.id, digest, file)
  }
}
