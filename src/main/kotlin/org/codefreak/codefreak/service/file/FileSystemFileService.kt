package org.codefreak.codefreak.service.file

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import java.util.stream.Stream
import kotlin.io.path.absolutePathString
import kotlin.io.path.isSymbolicLink
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.FileExistsException
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.file.PathUtils
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.util.FileUtil
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.TarUtil.entrySequence
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["codefreak.files.adapter"], havingValue = "FILE_SYSTEM")
class FileSystemFileService(@Autowired val config: AppConfiguration) : FileService {

  companion object {
    /**
     * This is a list names that should never be written to (directories and files).
     * They should never appear at any position in paths.
     */
    val blacklistedPaths = setOf(
      ".git",
      ".gitignore",
      ".gitattributes"
    )
  }

  init {
    val basePath = config.files.fileSystem.collectionStoragePath
    require(basePath.isNotBlank()) { "A collection storage path must be configured!" }
  }

  override fun readCollectionTar(collectionId: UUID): InputStream {
    val output = ByteArrayOutputStream()
    val tarOutput = TarUtil.PosixTarArchiveOutputStream(output)
    val collectionPath = createCollectionPath(collectionId)

    Files.walkFileTree(collectionPath, object : SimpleFileVisitor<Path>() {
      override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        dir?.let {
          val path = getFileRelativePath(collectionId, it)
          TarUtil.mkdir(path, tarOutput)
        }
        return FileVisitResult.CONTINUE
      }

      override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        file?.let {
          val path = getFileRelativePath(collectionId, it)
          TarUtil.writeFileWithContent(path, Files.newInputStream(it), tarOutput)
        }
        return FileVisitResult.CONTINUE
      }
    })

    tarOutput.close()

    return ByteArrayInputStream(output.toByteArray())
  }

  /**
   * Return the relative path of a file inside the specified collection.
   * The returned path has no leading file separator.
   */
  private fun getFileRelativePath(collectionId: UUID, path: Path): String {
    val fullPath = path.absolutePathString()
    val collectionPath = getCollectionPath(collectionId).absolutePathString()
    if (!fullPath.startsWith(collectionPath)) {
      throw IllegalArgumentException("$fullPath is not a file of collection $collectionId")
    }
    return fullPath.removePrefix(collectionPath).trimStart(File.separatorChar)
  }

  override fun writeCollectionTar(collectionId: UUID): OutputStream {
    return object : ByteArrayOutputStream() {
      override fun close() {
        val tempCollectionPath = Files.createTempDirectory("codefreak-extract-$collectionId")

        try {
          // Create the temp directory first so it still can be "moved" if the archive is empty
          Files.createDirectories(tempCollectionPath)

          // Extract the archive to a temporary location first so the current collection files are not lost if an error occurs whilst extraction
          TarArchiveInputStream(ByteArrayInputStream(toByteArray())).use { input ->
            input.entrySequence().forEach {
              val tempPath = FileUtil.resolveSecurely(tempCollectionPath, it.name)

              if (!isBlacklistedPath(tempPath)) {
                if (it.isFile) {
                  require(!Files.isDirectory(tempPath)) { "`${it.name}` is a directory" }

                  if (it.size == 0L) {
                    createFile(tempPath)
                  } else {
                    writeFile(tempPath).use { outputStream ->
                      IOUtils.copy(input, outputStream)
                    }
                  }
                } else if (it.isDirectory) {
                  createDirectory(tempPath)
                }
              }
            }
          }

          deleteCollection(collectionId)
          // Overwrite the existing collection with the tar contents
          FileUtils.moveDirectory(tempCollectionPath.toFile(), getCollectionPath(collectionId).toFile())
        } finally {
          FileUtils.deleteDirectory(tempCollectionPath.toFile())
        }
      }
    }
  }

  private fun isBlacklistedPath(path: Path) = blacklistedPaths.any {
    path.toString().split(path.fileSystem.separator).contains(it)
  }

  override fun collectionExists(collectionId: UUID): Boolean {
    val collectionPath = getCollectionPath(collectionId)
    return Files.isDirectory(collectionPath)
  }

  override fun deleteCollection(collectionId: UUID) {
    val collectionPath = getCollectionPath(collectionId)

    if (Files.isDirectory(collectionPath)) {
      PathUtils.deleteDirectory(collectionPath)
    }
  }

  override fun walkFileTree(collectionId: UUID): Stream<FileMetaData> {
    val collectionPath = createCollectionPath(collectionId)

    return Files.walk(collectionPath)
      .filter { it != collectionPath && !isBlacklistedPath(it) && !it.isSymbolicLink() }
      .map { filePathToFileMetaData(collectionId, it) }
  }

  private fun filePathToFileMetaData(collectionId: UUID, path: Path): FileMetaData {
    val relativePath = getFileRelativePath(collectionId, path)
    return FileMetaData(
      path = "/${FilenameUtils.normalize(relativePath, true)}",
      lastModifiedDate = Files.getLastModifiedTime(path).toInstant(),
      type = when {
        Files.isDirectory(path) -> FileType.DIRECTORY
        else -> FileType.FILE
      },
      size = Files.size(path),
      mode = FileUtil.getFileMode(path)
    )
  }

  override fun listFiles(collectionId: UUID, path: String): Stream<FileMetaData> {
    val fileTreeBasePath = getCollectionFilePath(collectionId, path)
    require(Files.isDirectory(fileTreeBasePath)) { "`$path` does not exist" }
    return Files.walk(fileTreeBasePath, 1)
      .filter { it != fileTreeBasePath && !isBlacklistedPath(it) && !it.isSymbolicLink() }
      .map { filePathToFileMetaData(collectionId, it) }
  }

  override fun createFiles(collectionId: UUID, paths: Set<String>) {
    paths.forEach {
      val path = getCollectionFilePath(collectionId, it)
      require(!isBlacklistedPath(path)) { "`$it` is not a valid path" }
      require(!Files.isDirectory(path)) { "`$it` is a directory" }
      createFile(path)
    }
  }

  private fun createFile(path: Path) {
    try {
      Files.createDirectories(path.parent)
      Files.createFile(path)
    } catch (e: FileAlreadyExistsException) {
      // Ignore silently
    }
  }

  override fun createDirectories(collectionId: UUID, paths: Set<String>) {
    paths.forEach {
      val path = getCollectionFilePath(collectionId, it)
      require(!isBlacklistedPath(path)) { "`$it` is not a valid path" }
      createDirectory(path)
    }
  }

  private fun createDirectory(path: Path) {
    require(!Files.isRegularFile(path)) { "`$path is a file`" }

    try {
      Files.createDirectories(path)
    } catch (e: FileAlreadyExistsException) {
      // Ignore silently
    }
  }

  override fun containsFile(collectionId: UUID, path: String): Boolean {
    val filePath = getCollectionFilePath(collectionId, path)
    return Files.exists(filePath) && Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)
  }

  override fun containsDirectory(collectionId: UUID, path: String): Boolean {
    val directoryPath = getCollectionFilePath(collectionId, path)
    return Files.exists(directoryPath) && Files.isDirectory(directoryPath, LinkOption.NOFOLLOW_LINKS)
  }

  override fun deleteFiles(collectionId: UUID, paths: Set<String>) {
    val sanitizedPaths = paths.map {
      val path = getCollectionFilePath(collectionId, it)
      require(Files.exists(path)) { "`$it` does not exist" }
      path
    }

    sanitizedPaths.forEach {
      FileUtils.forceDelete(it.toFile())
    }
  }

  override fun renameFile(collectionId: UUID, source: String, target: String) {
    val sourcePath = getCollectionFilePath(collectionId, source)
    val targetPath = getCollectionFilePath(collectionId, target)

    if (arePathsEqual(sourcePath, targetPath)) {
      // Renaming file to itself, ignore
      return
    }

    require(!isBlacklistedPath(targetPath)) { "The target path `$target` is not a valid path" }
    require(!Files.exists(targetPath)) { "The target path `$target` already exists" }

    val sourceFile = sourcePath.toFile()
    val targetFile = targetPath.toFile()

    when {
      Files.isRegularFile(sourcePath) -> FileUtils.moveFile(sourceFile, targetFile)
      Files.isDirectory(sourcePath) -> FileUtils.moveDirectory(sourceFile, targetFile)
      else -> throw IllegalArgumentException("The source path `$source` does not exist")
    }
  }

  override fun moveFile(collectionId: UUID, sources: Set<String>, target: String) {
    require(sources.isNotEmpty()) { "No sources given" }

    val targetPath = getCollectionFilePath(collectionId, target)
    require(Files.isDirectory(targetPath)) { "`$target` is not a directory" }

    val sourcePaths: Map<String, Path> = sources.mapNotNull {
      val sourcePath = getCollectionFilePath(collectionId, it)

      require(Files.exists(sourcePath)) { "The source path `$it` does not exist" }

      val mappedTargetPath = getCollectionFilePath(collectionId, target, FileUtil.basename(it))
      val shouldMovePath =
        !arePathsEqual(sourcePath, targetPath) && !arePathsEqual(sourcePath, mappedTargetPath) || Files.isDirectory(
          sourcePath
        )

      if (shouldMovePath) {
        Pair(it, sourcePath)
      } else {
        null
      }
    }.toMap()

    if (sourcePaths.isEmpty()) {
      // Nothing to move
      return
    }

    require(!isBlacklistedPath(targetPath)) { "The target path `$target` is not a valid path" }
    require(Files.isDirectory(targetPath)) { "`$target` is not a directory" }

    sourcePaths.forEach { (originalPath, sourcePath) ->
      try {
        FileUtils.moveToDirectory(sourcePath.toFile(), targetPath.toFile(), false)
      } catch (e: FileExistsException) {
        throw IllegalArgumentException("`$originalPath` or a child of it already exists in `$targetPath`")
      } catch (e: IOException) {
        throw IllegalArgumentException("The target `$target` is a descendant of the source `$originalPath`")
      }
    }
  }

  private fun arePathsEqual(path1: Path, path2: Path): Boolean {
    return path1.toString() == path2.toString() ||
        path1.parent.toString() == path2.toString() ||
        path1.toString() == path2.parent.toString()
  }

  override fun writeFile(collectionId: UUID, path: String): OutputStream {
    val filePath = getCollectionFilePath(collectionId, path)

    require(!isBlacklistedPath(filePath)) { "`$path` is not a valid path" }
    require(!Files.isDirectory(filePath)) { "`$path` is a directory" }

    return writeFile(filePath)
  }

  private fun writeFile(path: Path): OutputStream {
    Files.createDirectories(path.parent)
    // The default behaviour is StandardOpenOption::CREATE, StandardOpenOption::TRUNCATE_EXISTING and StandardOpenOption::WRITE
    return Files.newOutputStream(path)
  }

  override fun readFile(collectionId: UUID, path: String): InputStream {
    val filePath = getCollectionFilePath(collectionId, path)

    require(Files.isRegularFile(filePath)) { "`$path` does not exist or is a directory" }

    return Files.newInputStream(filePath)
  }

  /**
   * Convert a collection-relative path to an absolute path.
   * Path will be escaped properly
   */
  private fun getCollectionFilePath(collectionId: UUID, vararg pathSegments: String): Path {
    val path = pathSegments.joinToString(separator = "/")
    // join collection path and the relative file path to an absolute path
    return FileUtil.resolveSecurely(createCollectionPath(collectionId), path)
  }

  /**
   * Creates the directory path for the collection or simply returns it if it already exists.
   */
  private fun createCollectionPath(collectionId: UUID): Path {
    val collectionPath = getCollectionPath(collectionId)
    return Files.createDirectories(collectionPath)
  }

  private fun getCollectionPath(collectionId: UUID): Path {
    val basePath = config.files.fileSystem.collectionStoragePath
    return Paths.get(basePath, collectionId.toString())
  }
}
