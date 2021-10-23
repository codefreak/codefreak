package org.codefreak.codefreak.service.workspace

import java.io.InputStream
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Interface for services that provide and save files for workspaces.
 * The underlying file-structure for the streams are tar archives.
 */
interface WorkspaceFileService {
  /**
   * Provide a tar archive to the consumer that will be written to the workspace.
   */
  fun loadFiles(identifier: WorkspaceIdentifier, filesConsumer: Consumer<InputStream>)

  /**
   * Accept the files read from the workspace to store it back to the database.
   */
  fun saveFiles(identifier: WorkspaceIdentifier, filesSupplier: Supplier<InputStream>)
}
