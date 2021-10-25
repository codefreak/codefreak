package org.codefreak.codefreak.service.workspace

import java.io.InputStream
import java.util.UUID
import java.util.function.Consumer
import java.util.function.Supplier
import org.codefreak.codefreak.service.evaluation.EvaluationStepService
import org.codefreak.codefreak.service.workspace.CompositeWorkspaceFileService.ScopedFileWorkspaceService
import org.codefreak.codefreak.service.workspace.WorkspacePurpose.EVALUATION
import org.springframework.stereotype.Service

/**
 * Workspace file service for evaluations that will load the files via [EvaluationStepService].
 */
@Service
class WorkspaceEvaluationFileService(
  private val evaluationStepService: EvaluationStepService
) : ScopedFileWorkspaceService {
  override val supportedPurposes = setOf(EVALUATION)

  override fun loadFiles(identifier: WorkspaceIdentifier, filesConsumer: Consumer<InputStream>) {
    evaluationStepService.getFilesForEvaluation(UUID.fromString(identifier.reference)).use(filesConsumer::accept)
  }

  override fun saveFiles(identifier: WorkspaceIdentifier, filesSupplier: Supplier<InputStream>) {
    // Saving files from evaluations back to database is not supported
  }
}
