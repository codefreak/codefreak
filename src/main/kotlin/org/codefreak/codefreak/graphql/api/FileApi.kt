package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import java.nio.charset.Charset
import java.time.Instant
import java.util.Base64
import java.util.UUID
import kotlin.streams.toList
import org.apache.catalina.core.ApplicationPart
import org.apache.commons.io.IOUtils
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.TaskService
import org.codefreak.codefreak.service.file.FileContentService
import org.codefreak.codefreak.service.file.FileMetaData
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.exhaustive
import org.codefreak.codefreak.util.withTrailingSlash
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
  val type: FileDtoType,
  val lastModified: Instant,
  val size: Long?,
  val mode: Int?
) {
  constructor(collectionId: UUID, collectionDigest: ByteArray, virtualFile: FileContentService.VirtualFile) : this(
    collectionId,
    Base64.getEncoder().encodeToString(collectionDigest),
    path = virtualFile.path,
    // should use base64 encoding to transfer non-textual data
    // text is always treated as utf-8
    content = virtualFile.content?.toString(Charset.forName("UTF-8")),
    type = FileDtoType.valueOf(virtualFile.type.name),
    lastModified = virtualFile.lastModifiedDate,
    size = virtualFile.size,
    mode = virtualFile.mode
  )

  constructor(collectionId: UUID, collectionDigest: ByteArray, file: FileMetaData) : this(
    collectionId,
    Base64.getEncoder().encodeToString(collectionDigest),
    path = file.path,
    // no transfer of file content over GraphQL. The property can be dropped once we migrated fully to the new api
    content = null,
    type = FileDtoType.valueOf(file.type.name),
    lastModified = file.lastModifiedDate,
    size = file.size,
    mode = file.mode
  )
}

enum class FileContextType {
  TASK,
  ANSWER
}

data class FileContext(var type: FileContextType, var id: UUID)

@Component
class FileQuery : BaseResolver(), Query {
  fun answerFiles(answerId: UUID): List<FileDto> = context {
    val answer = serviceAccess.getService(AnswerService::class).findAnswer(answerId)
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

  fun listFiles(fileContext: FileContext, path: String = "/"): List<FileDto> = context {
    authorize(fileContext, FileActionType.VIEW)
    val fileService = serviceAccess.getService(FileService::class)
    val digest = fileService.getCollectionMd5Digest(fileContext.id)
    fileService.listFiles(fileContext.id, path).use { files ->
      files.map {
        FileDto(fileContext.id, digest, it)
      }.toList()
    }
  }
}

@Component
class FileMutation : BaseResolver(), Mutation {
  fun createFile(fileContext: FileContext, path: String): Boolean = context {
    authorize(fileContext, FileActionType.EDIT)
    serviceAccess.getService(FileService::class).createFiles(fileContext.id, setOf(path))
    true
  }

  fun createDirectory(fileContext: FileContext, path: String): Boolean = context {
    authorize(fileContext, FileActionType.EDIT)
    serviceAccess.getService(FileService::class).createDirectories(fileContext.id, setOf(path))
    true
  }

  fun uploadFiles(fileContext: FileContext, dir: String, files: Array<ApplicationPart>): Boolean = context {
    authorize(fileContext, FileActionType.EDIT)
    val fileService = serviceAccess.getService(FileService::class)
    files.forEach { file ->
      val filename = file.submittedFileName ?: "upload-${Instant.now()}-${file.name}"
      val filePath = "${dir.withTrailingSlash()}$filename"
      fileService.writeFile(fileContext.id, filePath).use {
        IOUtils.copy(file.inputStream, it)
      }
    }
    true
  }

  fun moveFiles(fileContext: FileContext, sources: List<String>, target: String): Boolean = context {
    authorize(fileContext, FileActionType.EDIT)
    serviceAccess.getService(FileService::class).moveFile(fileContext.id, sources.toSet(), target)
    true
  }

  fun renameFile(fileContext: FileContext, source: String, target: String): Boolean = context {
    authorize(fileContext, FileActionType.EDIT)
    serviceAccess.getService(FileService::class).renameFile(fileContext.id, source, target)
    true
  }

  fun deleteFiles(fileContext: FileContext, paths: List<String>): Boolean = context {
    authorize(fileContext, FileActionType.EDIT)
    serviceAccess.getService(FileService::class).deleteFiles(fileContext.id, paths.toSet())
    true
  }
}

enum class FileActionType {
  VIEW,
  EDIT
}

private fun BaseResolver.authorize(fileContext: FileContext, fileAction: FileActionType) = context {
  when (fileContext.type) {
    FileContextType.ANSWER -> {
      val answer = serviceAccess.getService(AnswerService::class).findAnswer(fileContext.id)
      authorization.requireAuthorityIfNotCurrentUser(answer.submission.user, Authority.ROLE_ADMIN)

      if (fileAction == FileActionType.EDIT) {
        require(answer.isEditable) { "The answer is not editable anymore" }
      }

      return@context
    }
    FileContextType.TASK -> {
      val task = serviceAccess.getService(TaskService::class).findTask(fileContext.id)
      authorization.requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
    }
  }.exhaustive
}
