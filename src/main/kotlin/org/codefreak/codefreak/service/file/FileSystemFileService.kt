package org.codefreak.codefreak.service.file

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.util.FileUtil
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.TarUtil.entrySequence
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Service
@ConditionalOnProperty(name = ["codefreak.files.adapter"], havingValue = "FILE_SYSTEM")
class FileSystemFileService(@Autowired val config: AppConfiguration) : FileService {

  override fun readCollectionTar(collectionId: UUID): InputStream {
    val output = ByteArrayOutputStream()
    val tarOutput = TarArchiveOutputStream(output)
    val collectionPath = getCollectionPath(collectionId)

    Files.walkFileTree(collectionPath, object : SimpleFileVisitor<Path>() {
      override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        dir?.let {
          val path = collectionPath.relativize(dir).normalize()
          val normalizedPath = TarUtil.normalizeDirectoryName(path.toString())
          TarUtil.mkdir(normalizedPath, tarOutput)
        }
        return FileVisitResult.CONTINUE
      }

      override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        file?.let {
          val path = collectionPath.relativize(file).normalize()
          val normalizedPath = TarUtil.normalizeFileName(path.toString())
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
        // Overwrite the existing collection with the tar contents
        deleteCollection(collectionId)

        TarArchiveInputStream(ByteArrayInputStream(toByteArray())).use { input ->
          input.entrySequence().forEach {
            if (it.isFile) {
              if (it.size == 0L) {
                createFiles(collectionId, setOf(it.name))
              } else {
                writeFile(collectionId, it.name).use { outputStream ->
                  IOUtils.copy(input, outputStream)
                }
              }
            } else if (it.isDirectory) {
              createDirectories(collectionId, setOf(it.name))
            }
          }
        }
      }
    }
  }

  override fun collectionExists(collectionId: UUID): Boolean {
    val collectionPath = getCollectionPath(collectionId)
    return Files.exists(collectionPath)
  }

  override fun deleteCollection(collectionId: UUID) {
    val collectionPath = getCollectionPath(collectionId)

    if (Files.exists(collectionPath) && Files.isDirectory(collectionPath)) {
      deleteDirectoryRecursively(collectionPath)
    }
  }

  override fun walkFileTree(collectionId: UUID): Sequence<FileMetaData> {
    val collectionPath = getCollectionPath(collectionId)
    val fileTree = mutableListOf<FileMetaData>()

    Files.walkFileTree(collectionPath, object : SimpleFileVisitor<Path>() {
      override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        file?.let { fileTree.add(filePathToFileMetaData(it, collectionPath)) }
        return FileVisitResult.CONTINUE
      }

      override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
        dir?.let { fileTree.add(filePathToFileMetaData(it, collectionPath)) }
        return FileVisitResult.CONTINUE
      }
    })

    return fileTree.asSequence()
  }

  private fun filePathToFileMetaData(path: Path, basePath: Path): FileMetaData {
    val relativizedPath = basePath.relativize(path)
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
    val collectionPath = getCollectionPath(collectionId)
    val fileTreeBasePath = Paths.get(collectionPath.toString(), path)

    require(Files.exists(fileTreeBasePath)) { "`$path` does not exist" }

    val fileTree = mutableListOf<FileMetaData>()

    Files.walkFileTree(fileTreeBasePath, object : SimpleFileVisitor<Path>() {
      override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        file?.let {
          if (it.parent == fileTreeBasePath) {
            fileTree.add(filePathToFileMetaData(it, collectionPath))
          }
        }
        return FileVisitResult.CONTINUE
      }

      override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
        dir?.let {
          if (it.parent == fileTreeBasePath) {
            fileTree.add(filePathToFileMetaData(it, collectionPath))
          }
        }
        return FileVisitResult.CONTINUE
      }
    })

    return fileTree.asSequence()
  }

  override fun createFiles(collectionId: UUID, paths: Set<String>) {
    val collectionPath = getCollectionPath(collectionId).toString()
    paths.forEach {
      val normalizedPathName = FileUtil.normalizeName(it)
      require(normalizedPathName.isNotBlank()) { "`$it` is not a valid path" }
      val filePath = Paths.get(collectionPath, normalizedPathName)
      createFile(filePath)
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
    val collectionPath = getCollectionPath(collectionId).toString()
    paths.forEach {
      val normalizedPathName = FileUtil.normalizeName(it)
      require(normalizedPathName.isNotBlank() || TarUtil.isRoot(normalizedPathName)) { "`$it` is not a valid path" }
      val directoryPath = Paths.get(collectionPath, normalizedPathName)
      createDirectory(directoryPath)
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
    val collectionPath = getCollectionPath(collectionId).toString()
    val normalizedPath = FileUtil.normalizeName(path)
    val filePath = Paths.get(collectionPath, normalizedPath)
    return Files.exists(filePath) && Files.isRegularFile(filePath)
  }

  override fun containsDirectory(collectionId: UUID, path: String): Boolean {
    val collectionPath = getCollectionPath(collectionId).toString()
    val normalizedPath = FileUtil.normalizeName(path)
    val filePath = Paths.get(collectionPath, normalizedPath)
    return Files.exists(filePath) && Files.isDirectory(filePath)
  }

  override fun deleteFiles(collectionId: UUID, paths: Set<String>) {
    val collectionPath = getCollectionPath(collectionId).toString()

    val normalizedPaths = paths.map {
      val normalizedPathName = FileUtil.normalizeName(it)
      require(normalizedPathName.isNotBlank()) { "`$it` is not a valid path" }
      val filePath = Paths.get(collectionPath, normalizedPathName)
      require(Files.exists(filePath)) { "`$filePath` does not exist" }
      filePath
    }

    normalizedPaths.forEach {
      deleteFile(it)
    }
  }

  private fun deleteFile(path: Path) {
    require(Files.exists(path)) { "`$path` does not exist" }

    try {
      Files.delete(path)
    } catch (e: DirectoryNotEmptyException) {
      deleteDirectoryRecursively(path)
    }
  }

  private fun deleteDirectoryRecursively(path: Path) {
    require(Files.exists(path)) { "`$path` does not exist" }

    Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
      override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        file?.let { Files.delete(it) }
        return FileVisitResult.CONTINUE
      }

      override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
        exc?.let { throw it }
        dir?.let { Files.delete(it) }
        return FileVisitResult.CONTINUE
      }
    })
  }

  override fun moveFile(collectionId: UUID, sources: Set<String>, target: String) {
    require(sources.isNotEmpty()) { "No sources given" }

    val collectionPath = getCollectionPath(collectionId)
    val targetPath = Paths.get(collectionPath.toString(), target)

    val sourcePaths = mutableListOf<Path>()
    sources.forEach {
      val sourcePath = Paths.get(collectionPath.toString(), it)
      require(Files.exists(sourcePath)) { "The source path `$it` does not exist" }

      val targetPath2 = Paths.get(targetPath.toString(), it)
      if (!arePathsEqual(sourcePath, targetPath) && !arePathsEqual(sourcePath, targetPath2) || Files.isDirectory(sourcePath)) {
        sourcePaths.add(sourcePath)
      }
    }

    if (sourcePaths.isEmpty()) {
      return
    }

    if (!Files.exists(targetPath)) {
      require(sourcePaths.size == 1) { "`$target` is not a directory" }
      val sourcePath = sourcePaths.first()
      when {
        Files.isRegularFile(sourcePath) -> moveSingleFile(sourcePath, targetPath)
        Files.isDirectory(sourcePath) -> moveDirectoryRecursively(sourcePath, targetPath)
        else -> throw IllegalStateException("`$sourcePath` does not exist though it should at this point")
      }
      return
    }

    require(
      Files.isDirectory(targetPath)
          || (sourcePaths.size == 1 && arePathsEqual(sourcePaths.first(), targetPath))
    ) { "`$target` is not a directory" }

    sourcePaths.forEach {
      require(!isDescendant(targetPath, it)) { "The target `$target` is a descendant of the source `$it`" }
      require(!isBasenamePresent(it, targetPath)) { "`$it` or a child of it already exists in `$targetPath`" }
    }

    sourcePaths.forEach {
      when {
        Files.isRegularFile(it) -> moveSingleFile(it, targetPath)
        Files.isDirectory(it) -> moveDirectoryRecursively(it, targetPath)
        else -> throw IllegalStateException("`$it` does not exist though it should at this point")
      }
    }
  }

  private fun moveSingleFile(source: Path, target: Path) {
    require(Files.isRegularFile(source)) { "`$source` is not a file" }

    if (Files.exists(target)) {
      require(
        Files.isDirectory(target) || arePathsEqual(source, target)
      ) { "The target path `$target` already exists" }
    }

    var targetPath = target

    if (Files.isDirectory(target)) {
      targetPath = Paths.get(target.toString(), source.fileName.toString())
    }

    Files.move(source, targetPath)
  }

  private fun isDescendant(path: Path, of: Path): Boolean {
    return path.toString().contains(of.toString())
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
    return path1.toString() == path2.toString()
        || path1.parent.toString() == path2.toString()
        || path1.toString() == path2.parent.toString()
  }

  private fun moveDirectoryRecursively(source: Path, target: Path) {
    require(Files.isDirectory(source)) { "`$source` is not a directory" }

    source.let {
      val targetPath = Paths.get(target.toString(), it.fileName.toString())
      Files.createDirectories(targetPath)
    }

    Files.walkFileTree(source, object : SimpleFileVisitor<Path>() {
      override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        dir?.let {
          // Create the directory in the target so its files can be copied there
          val sourceDir = source.relativize(it).normalize()
          val targetDir = Paths.get(target.toString(), sourceDir.toString())
          Files.createDirectories(targetDir)
        }
        return FileVisitResult.CONTINUE
      }

      override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        file?.let {
          // Move the file to the new location
          val sourcePath = source.relativize(it).normalize()
          val targetPath = Paths.get(target.toString(), sourcePath.toString())
          moveSingleFile(it, targetPath)
        }
        return FileVisitResult.CONTINUE
      }

      override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
        dir?.let {
          // Delete directory after moving all its contents
          Files.delete(it)
        }
        return FileVisitResult.CONTINUE
      }
    })
  }

  override fun writeFile(collectionId: UUID, path: String): OutputStream {
    val collectionPath = getCollectionPath(collectionId)
    val filePath = Paths.get(collectionPath.toString(), path)

    require(!Files.isDirectory(filePath)) { "`$path` is a directory" }

    return Files.newOutputStream(filePath)
  }

  override fun readFile(collectionId: UUID, path: String): InputStream {
    val collectionPath = getCollectionPath(collectionId)
    val filePath = Paths.get(collectionPath.toString(), path)

    require(Files.isRegularFile(filePath)) { "`$path` does not exist or is a directory" }

    return Files.newInputStream(filePath)
  }

  private fun getCollectionPath(collectionId: UUID): Path {
    val basePath = config.files.fileSystem.collectionStoragePath
    val collectionPath = Paths.get(basePath, collectionId.toString())
    return Files.createDirectories(collectionPath)
  }
}
