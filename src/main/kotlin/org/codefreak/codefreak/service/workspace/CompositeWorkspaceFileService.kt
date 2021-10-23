package org.codefreak.codefreak.service.workspace

import java.io.InputStream
import java.util.function.Consumer
import java.util.function.Supplier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

/**
 * WorkspaceFileService that delegates all save and load to a file service responsible for specific `WorkspacePurpose`s.
 * This is the primary file service but actual save and load operations are implemented in the following classes:
 * * [WorkspaceIdeFileService]
 * * [WorkspaceEvaluationFileService]
 */
@Primary
@Service
class CompositeWorkspaceFileService(
  delegates: List<ScopedFileWorkspaceService>
) : WorkspaceFileService {
  /**
   * Small extended interface that adds a list of supported WorkspacePurpose to each file service.
   */
  interface ScopedFileWorkspaceService : WorkspaceFileService {
    val supportedPurposes: Set<WorkspacePurpose>
  }

  /**
   * Create a map of WorkspacePurpose to WorkspaceFileService by the list of delegates.
   * Duplicates are ignored silently.
   */
  private val delegatedFileServices: Map<WorkspacePurpose, WorkspaceFileService> =
    delegates.flatMap { delegate -> delegate.supportedPurposes.map { purpose -> Pair(purpose, delegate) } }.toMap()

  override fun loadFiles(identifier: WorkspaceIdentifier, filesConsumer: Consumer<InputStream>) {
    getDelegate(identifier).loadFiles(identifier, filesConsumer)
  }

  override fun saveFiles(identifier: WorkspaceIdentifier, filesSupplier: Supplier<InputStream>) {
    getDelegate(identifier).saveFiles(identifier, filesSupplier)
  }

  private fun getDelegate(identifier: WorkspaceIdentifier): WorkspaceFileService {
    return delegatedFileServices[identifier.purpose]
      ?: throw IllegalArgumentException("Workspace purpose ${identifier.purpose} is not supported")
  }
}
