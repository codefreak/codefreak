package org.codefreak.codefreak.service.file

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import kotlin.io.path.isSymbolicLink
import kotlin.streams.asSequence
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.io.FileUtils
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
    val tarOutput = TarArchiveOutputStream(output)
    val collectionPath = createCollectionPath(collectionId)

    Files.walkFileTree(collectionPath, object : SimpleFileVisitor<Path>() {
      override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        dir?.let {
          val sanitizedPath = Paths.get("/", FileUtil.sanitizeName(it.toString()))
          val path = collectionPath.relativize(sanitizedPath).normalize().toString()
          val normalizedPath = TarUtil.normalizeDirectoryName(path).replace("$collectionId/", "")
          TarUtil.mkdir(normalizedPath, tarOutput)
        }
        return FileVisitResult.CONTINUE
      }

      override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        file?.let {
          val sanitizedPath = Paths.get("/", FileUtil.sanitizeName(it.toString()))
          val path = collectionPath.relativize(sanitizedPath).normalize().toString()
          val normalizedPath = TarUtil.normalizeFileName(path).replace("$collectionId/", "")
          TarUtil.writeFileWithContent(normalizedPath, Files.newInputStream(it), tarOutput)
        }
        return FileVisitResult.CONTINUE
      }
    })

    tarOutput.close()

    return ByteArrayInputStream(output.toByteArray())
  }

  override fun writeCollectionTar(collectionId: UUID): OutputStream {
    return object : ByteArrayOutputStream() {
      override fun close() {
        val collectionPath = createCollectionPath(collectionId)
        val tempCollectionPath = Paths.get(FileUtils.getTempDirectory().path, "codefreak", collectionId.toString())
        // Create the temp directory first so it still can be "moved" if the archive is empty
        Files.createDirectories(tempCollectionPath)

        // Extract the archive to a temporary location first so the current collection files are not lost if an error occurs whilst extraction
        TarArchiveInputStream(ByteArrayInputStream(toByteArray())).use { input ->
          input.entrySequence().forEach {
            val sanitizedName = FileUtil.sanitizeName(it.name)
            val path = Paths.get(collectionPath.toString(), sanitizedName)
            val tempPath = Paths.get(tempCollectionPath.toString(), sanitizedName)

            require(!isBlacklistedPath(collectionId, path)) { "`${it.name}` is not a valid path" }

            if (it.isFile) {
              // Create parent directories first because they might only implicitly exist in the tar through this file name
              Files.createDirectories(tempPath.parent)

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

        // Overwrite the existing collection with the tar contents
        deleteCollection(collectionId)
        FileUtils.moveToDirectory(tempCollectionPath.toFile(), collectionPath.parent.toFile(), true)
        FileUtils.deleteDirectory(tempCollectionPath.toFile())
      }
    }
  }

  private fun isBlacklistedPath(collectionId: UUID, path: Path): Boolean {
    val collectionPath = createCollectionPath(collectionId)

    blacklistedPaths.forEach {
      val blacklistedPath = Paths.get(collectionPath.toString(), it)
      if (path.startsWith(blacklistedPath)) {
        return true
      }
    }

    return false
  }

  override fun collectionExists(collectionId: UUID): Boolean {
    val collectionPath = getCollectionPath(collectionId)
    return Files.exists(collectionPath)
  }

  override fun deleteCollection(collectionId: UUID) {
    val collectionPath = createCollectionPath(collectionId)

    if (Files.exists(collectionPath) && Files.isDirectory(collectionPath)) {
      PathUtils.deleteDirectory(collectionPath)
    }
  }

  override fun walkFileTree(collectionId: UUID): Sequence<FileMetaData> {
    val collectionPath = createCollectionPath(collectionId)

    return Files.walk(collectionPath)
      .filter { it != collectionPath && !isBlacklistedPath(collectionId, it) && !it.isSymbolicLink() }
      .map { filePathToFileMetaData(it, collectionPath) }
      .asSequence()
  }

  private fun filePathToFileMetaData(path: Path, basePath: Path): FileMetaData {
    val relativizedPath = basePath.relativize(path).normalize()
    return FileMetaData(
      path = "/$relativizedPath",
      lastModifiedDate = Files.getLastModifiedTime(path).toInstant(),
      type = when {
        Files.isDirectory(path) -> FileType.DIRECTORY
        else -> FileType.FILE
      },
      size = Files.size(path),
      mode = FileUtil.getFilePermissionsMode(Files.getPosixFilePermissions(path))
    )
  }

  override fun listFiles(collectionId: UUID, path: String): Sequence<FileMetaData> {
    val collectionPath = createCollectionPath(collectionId)
    val fileTreeBasePath = Paths.get(collectionPath.toString(), path)

    require(Files.exists(fileTreeBasePath)) { "`$path` does not exist" }

    return Files.walk(fileTreeBasePath, 1)
      .filter { it != collectionPath && !isBlacklistedPath(collectionId, it) && !it.isSymbolicLink() }
      .map { filePathToFileMetaData(it, collectionPath) }
      .asSequence()
  }

  override fun createFiles(collectionId: UUID, paths: Set<String>) {
    val collectionPath = createCollectionPath(collectionId).toString()
    paths.forEach {
      val path = Paths.get(collectionPath, FileUtil.sanitizeName(it))
      require(!isBlacklistedPath(collectionId, path)) { "`$it` is not a valid path" }
      createFile(path)
    }
  }

  private fun createFile(path: Path) {
    require(!Files.isDirectory(path)) { "`$path` is a directory" }

    try {
      Files.createFile(path)
    } catch (e: IOException) {
      throw IllegalArgumentException("A parent directory of $path does not exist")
    } catch (e: FileAlreadyExistsException) {
      // Ignore silently
    }
  }

  override fun createDirectories(collectionId: UUID, paths: Set<String>) {
    val collectionPath = createCollectionPath(collectionId).toString()
    paths.forEach {
      val path = Paths.get(collectionPath, FileUtil.sanitizeName(it))
      require(!isBlacklistedPath(collectionId, path)) { "`$it` is not a valid path" }
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
    val collectionPath = createCollectionPath(collectionId).toString()
    val filePath = Paths.get(collectionPath, FileUtil.sanitizeName(path))
    return Files.exists(filePath) && Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS) && !isBlacklistedPath(collectionId, filePath)
  }

  override fun containsDirectory(collectionId: UUID, path: String): Boolean {
    val collectionPath = createCollectionPath(collectionId).toString()
    val directoryPath = Paths.get(collectionPath, FileUtil.sanitizeName(path))
    return Files.exists(directoryPath) && Files.isDirectory(directoryPath, LinkOption.NOFOLLOW_LINKS) && !isBlacklistedPath(collectionId, directoryPath)
  }

  override fun deleteFiles(collectionId: UUID, paths: Set<String>) {
    val collectionPath = createCollectionPath(collectionId).toString()

    val sanitizedPaths = paths.map {
      val path = Paths.get(collectionPath, FileUtil.sanitizeName(it))
      require(!isBlacklistedPath(collectionId, path)) { "`$it` is not a valid path" }
      require(Files.exists(path)) { "`$it` does not exist" }
      path
    }

    sanitizedPaths.forEach {
      FileUtils.forceDelete(it.toFile())
    }
  }

  override fun renameFile(collectionId: UUID, source: String, target: String) {
    val collectionPath = createCollectionPath(collectionId).toString()
    val sourcePath = Paths.get(collectionPath, source)
    val targetPath = Paths.get(collectionPath, target)

    require(Files.exists(sourcePath)) { "The source path `$source` does not exist" }
    require(!isBlacklistedPath(collectionId, sourcePath)) { "The source path `$source` is not a valid path" }

    if (arePathsEqual(sourcePath, targetPath)) {
      // Renaming file to itself, ignore
      return
    }

    require(!Files.exists(targetPath)) { "The target path `$target` already exists" }
    require(!isBlacklistedPath(collectionId, targetPath)) { "The target path `$target` is not a valid path" }

    val sourceFile = sourcePath.toFile()
    val targetFile = targetPath.toFile()

    when {
      Files.isRegularFile(sourcePath) -> FileUtils.moveFile(sourceFile, targetFile)
      Files.isDirectory(sourcePath) -> FileUtils.moveDirectory(sourceFile, targetFile)
      else -> throw IllegalStateException("`$sourcePath` does not exist though it should at this point")
    }
  }

  override fun moveFile(collectionId: UUID, sources: Set<String>, target: String) {
    require(sources.isNotEmpty()) { "No sources given" }

    val collectionPath = createCollectionPath(collectionId).toString()
    val targetPath = Paths.get(collectionPath, target)

    val sourcePaths = mutableListOf<Path>()
    sources.forEach {
      val sanitizedPath = FileUtil.sanitizeName(it)
      val sourcePath = Paths.get(collectionPath, sanitizedPath)
      require(Files.exists(sourcePath)) { "The source path `$it` does not exist" }
      require(!isBlacklistedPath(collectionId, sourcePath)) { "The source path `$it` is not a valid path" }

      val mappedTargetPath = Paths.get(targetPath.toString(), sanitizedPath)
      val shouldMovePath = !arePathsEqual(sourcePath, targetPath) && !arePathsEqual(sourcePath, mappedTargetPath) || Files.isDirectory(sourcePath)

      if (shouldMovePath) {
        sourcePaths.add(sourcePath)
      }
    }

    if (sourcePaths.isEmpty()) {
      // Nothing to move
      return
    }

    require(Files.isDirectory(targetPath)) { "`$target` is not a directory" }
    require(!isBlacklistedPath(collectionId, targetPath)) { "The target path `$target` is not a valid path" }

    sourcePaths.forEach {
      require(!isDescendant(targetPath, it)) { "The target `$target` is a descendant of the source `$it`" }
      require(!isBasenamePresent(it, targetPath)) { "`$it` or a child of it already exists in `$targetPath`" }
    }

    sourcePaths.forEach {
      FileUtils.moveToDirectory(it.toFile(), targetPath.toFile(), false)
    }
  }

  private fun isDescendant(path: Path, of: Path): Boolean {
    return path.toString().startsWith(of.toString())
  }

  private fun isBasenamePresent(source: Path, target: Path): Boolean {
    if (Files.isRegularFile(source)) {
      val path = Paths.get(target.toString(), source.fileName.toString())
      return Files.exists(path) && !arePathsEqual(source, target)
    }

    return Files.list(source).anyMatch {
      isBasenamePresent(it, target)
    }
  }

  private fun arePathsEqual(path1: Path, path2: Path): Boolean {
    return path1.toString() == path2.toString() ||
        path1.parent.toString() == path2.toString() ||
        path1.toString() == path2.parent.toString()
  }

  override fun writeFile(collectionId: UUID, path: String): OutputStream {
    val collectionPath = createCollectionPath(collectionId).toString()
    val filePath = Paths.get(collectionPath, FileUtil.sanitizeName(path))

    require(!isBlacklistedPath(collectionId, filePath)) { "`$path` is not a valid path" }

    return writeFile(filePath)
  }

  private fun writeFile(path: Path): OutputStream {
    require(!Files.isDirectory(path)) { "`$path` is a directory" }

    // The default behaviour is StandardOpenOption::CREATE, StandardOpenOption::TRUNCATE_EXISTING and StandardOpenOption::WRITE
    return Files.newOutputStream(path)
  }

  override fun readFile(collectionId: UUID, path: String): InputStream {
    val collectionPath = createCollectionPath(collectionId).toString()
    val filePath = Paths.get(collectionPath, FileUtil.sanitizeName(path))

    require(Files.isRegularFile(filePath)) { "`$path` does not exist or is a directory" }
    require(!isBlacklistedPath(collectionId, filePath)) { "`$path` is not a valid path" }

    return Files.newInputStream(filePath)
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
