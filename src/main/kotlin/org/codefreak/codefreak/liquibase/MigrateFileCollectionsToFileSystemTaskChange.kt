package org.codefreak.codefreak.liquibase

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.ResultSet
import liquibase.change.custom.CustomTaskChange
import liquibase.database.Database
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.ValidationErrors
import liquibase.resource.ResourceAccessor
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.codefreak.codefreak.util.TarUtil.entrySequence

class MigrateFileCollectionsToFileSystemTaskChange : CustomTaskChange {
  companion object {
    private const val SELECT_ALL_FILE_COLLECTIONS = "SELECT * FROM file_collection"
    private const val ID = "id"
    private const val TAR = "tar"
  }

  // The path is set through the Spring configuration at `spring.liquibase.parameters.collectionStoragePath`
  lateinit var collectionStoragePath: String

  private var resourceAccessor: ResourceAccessor? = null
  private lateinit var fileSystemPath: Path
  private var numberOfMigratedFileCollections = 0

  override fun getConfirmationMessage(): String {
    return when (numberOfMigratedFileCollections) {
      0 -> "No file-collections migrated to the filesystem"
      1 -> "$numberOfMigratedFileCollections file-collections migrated to `$fileSystemPath`"
      else -> "$numberOfMigratedFileCollections file-collections migrated to `$fileSystemPath`"
    }
  }

  override fun setUp() {
    fileSystemPath = Paths.get(collectionStoragePath)
  }

  override fun setFileOpener(resourceAccessor: ResourceAccessor?) {
    this.resourceAccessor = resourceAccessor
  }

  override fun validate(database: Database?): ValidationErrors {
    val errors = ValidationErrors()

    if (!Files.isDirectory(fileSystemPath)) {
      errors.addError("`$fileSystemPath` does not exist or is not a directory")
    } else if (!Files.isWritable(fileSystemPath)) {
      errors.addError("`$fileSystemPath` is not writable")
    }

    return errors
  }

  override fun execute(database: Database?) {
    database?.let {
      Files.createDirectories(fileSystemPath)

      val resultSet = getFileCollectionsFromDatabase(it)
      exportFileCollectionsToFileSystem(resultSet)
    }
  }

  private fun getFileCollectionsFromDatabase(database: Database): ResultSet {
    val connection = (database.connection as JdbcConnection).wrappedConnection
    val statement = connection.createStatement()
    return statement.executeQuery(SELECT_ALL_FILE_COLLECTIONS)
  }

  private fun exportFileCollectionsToFileSystem(resultSet: ResultSet) {
    generateSequence {
      if (resultSet.next()) resultSet else null
    }.forEach { row ->
      exportRowToFileSystem(row)
    }
  }

  private fun exportRowToFileSystem(row: ResultSet) {
    val id = row.getString(ID)
    val tarBlob = row.getBytes(TAR)
    val collectionPathName = fileSystemPath.resolve(id)

    Files.createDirectories(collectionPathName)

    TarArchiveInputStream(ByteArrayInputStream(tarBlob)).use { inputStream ->
      inputStream.entrySequence().forEach { entry ->
        exportEntryToFileSystem(entry, collectionPathName, inputStream)
      }
    }

    numberOfMigratedFileCollections++
  }

  private fun exportEntryToFileSystem(
    entry: TarArchiveEntry,
    collectionPathName: Path,
    inputStream: TarArchiveInputStream
  ) {
    val entryPath = collectionPathName.resolve(entry.name)

    if (entry.isFile) {
      if (entry.size > 0L) {
        Files.newOutputStream(entryPath).use { outputStream ->
          IOUtils.copy(inputStream, outputStream)
        }
      } else {
        Files.createFile(entryPath)
      }
    } else {
      Files.createDirectories(entryPath)
    }
  }
}
