package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import org.apache.catalina.core.ApplicationPart
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.auth.hasAuthority
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.IdeService
import org.codefreak.codefreak.service.TaskService
import org.codefreak.codefreak.service.file.FileContentService
import org.codefreak.codefreak.service.file.FileService
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.util.Base64
import java.util.UUID

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

enum class FileContextType {
  TASK,
  ANSWER
}

data class FileContext(var contextType: FileContextType, var id: UUID)

@Component
class FileQuery : BaseResolver(), Query {
  fun answerFiles(answerId: UUID): List<FileDto> = context {
    val answer = serviceAccess.getService(AnswerService::class).findAnswer(answerId)
    val forceSaveFiles = authorization.isCurrentUser(answer.task.owner) || authorization.currentUser.hasAuthority(Authority.ROLE_ADMIN)
    serviceAccess.getService(IdeService::class).saveAnswerFiles(answer, forceSaveFiles)
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

@Component
class FileMutation : BaseResolver(), Mutation {
  fun createFile(fileContext: FileContext, path: String): Boolean = context {
    authorize(fileContext)
    serviceAccess.getService(FileService::class).createFile(fileContext.id, path)
    true
  }

  fun createDirectory(fileContext: FileContext, path: String): Boolean = context {
    authorize(fileContext)
    serviceAccess.getService(FileService::class).createDirectory(fileContext.id, path)
    true
  }

  fun uploadFile(fileContext: FileContext, path: String, contents: ApplicationPart): Boolean = context {
    authorize(fileContext)
    serviceAccess.getService(FileService::class).filePutContents(fileContext.id, path).use {
      it.write(contents.inputStream.readBytes())
    }
    true
  }

  fun moveFile(fileContext: FileContext, sourcePath: String, targetPath: String): Boolean = context {
    authorize(fileContext)
    serviceAccess.getService(FileService::class).moveFile(fileContext.id, sourcePath, targetPath)
    true
  }

  fun moveDirectory(fileContext: FileContext, sourcePath: String, targetPath: String): Boolean = context {
    authorize(fileContext)
    serviceAccess.getService(FileService::class).moveDirectory(fileContext.id, sourcePath, targetPath)
    true
  }

  fun deleteFile(fileContext: FileContext, path: String): Boolean = context {
    authorize(fileContext)
    serviceAccess.getService(FileService::class).deleteFile(fileContext.id, path)
    true
  }

  fun deleteDirectory(fileContext: FileContext, path: String): Boolean = context {
    authorize(fileContext)
    serviceAccess.getService(FileService::class).deleteDirectory(fileContext.id, path)
    true
  }

  private fun authorize(fileContext: FileContext) = context {
    when (fileContext.contextType) {
      FileContextType.ANSWER -> {
        val answer = serviceAccess.getService(AnswerService::class).findAnswer(fileContext.id)
        authorization.requireAuthorityIfNotCurrentUser(answer.task.owner, Authority.ROLE_ADMIN)
      }
      FileContextType.TASK -> {
        val task = serviceAccess.getService(TaskService::class).findTask(fileContext.id)
        authorization.requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
      }
    }
    true
  }
}
