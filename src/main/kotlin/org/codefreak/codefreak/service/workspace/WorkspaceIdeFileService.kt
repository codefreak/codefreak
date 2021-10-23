package org.codefreak.codefreak.service.workspace

import java.io.InputStream
import java.util.UUID
import java.util.function.Consumer
import java.util.function.Supplier
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.service.workspace.CompositeWorkspaceFileService.ScopedFileWorkspaceService
import org.codefreak.codefreak.service.workspace.WorkspacePurpose.ANSWER_IDE
import org.codefreak.codefreak.service.workspace.WorkspacePurpose.TASK_IDE
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils

/**
 * Workspace file service that is responsible for answer and task IDEs. It tries to load/save by the UUID saved in
 * the workspace identifier reference field.
 */
@Service
class WorkspaceIdeFileService(
  private val fileService: FileService
) : ScopedFileWorkspaceService {
  override val supportedPurposes = setOf(ANSWER_IDE, TASK_IDE)

  override fun loadFiles(identifier: WorkspaceIdentifier, filesConsumer: Consumer<InputStream>) {
    fileService.readCollectionTar(UUID.fromString(identifier.reference)).use(filesConsumer::accept)
  }

  override fun saveFiles(identifier: WorkspaceIdentifier, filesSupplier: Supplier<InputStream>) {
    fileService.writeCollectionTar(UUID.fromString(identifier.reference)).use {
      StreamUtils.copy(filesSupplier.get(), it)
    }
  }
}
