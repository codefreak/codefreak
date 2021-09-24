package org.codefreak.codefreak.util

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import org.apache.commons.io.FilenameUtils
import org.springframework.util.AntPathMatcher

object FileUtil {

  private val matcher = AntPathMatcher()

  /**
   * Resolve an untrusted (user-input) path from the given base path.
   * Use this method to prevent directory traversal attacks.
   */
  fun resolveSecurely(basePath: Path, path: String): Path {
    return basePath.resolve(normalizePath(path))
  }

  /**
   * Get the last part of a given path including file extension.
   * /foo/bar.txt -> bar.txt
   * /foo/bar/ -> bar
   * / -> <empty string>
   * <empty string> -> <empty string>
   */
  fun basename(path: String): String {
    return FilenameUtils.getName(normalizePath(path))
  }

  /**
   * Remove leading dots and slashes from given path and normalizes patterns like `foo/../bar` to `bar`.
   * Always use this function on user input to prevent directory traversal attacks!
   * The returned value has no leading or trailing path separator.
   * Expected results are very similar to FilenameUtils.normalize, except the "no parent directory left" case:
   * ../a will return a
   * /a/../../b/c will return b/c
   */
  fun normalizePath(vararg name: String): String {
    var concated = name.joinToString(File.separator)
    // FilenameUtils.normalize returns null in case it has no parent directory left to work with
    // so "/../foo" will return null. We trick this by prepending fake directories to the original path
    // until we get a valid path from FilenameUtils.normalize.
    while (FilenameUtils.normalize(concated) === null) {
      concated = "a" + File.separatorChar + concated
    }
    return FilenameUtils.normalizeNoEndSeparator(concated).trimStart(File.separatorChar)
  }

  /**
   * Return the UNIX file permissions of a file.
   * In case we are not on a *NIX system this will return null.
   */
  fun getFileMode(path: Path): Int? {
    if (!path.fileSystem.supportedFileAttributeViews().contains("posix")) {
      return null
    }
    return Files.getPosixFilePermissions(path).sumOf(FileUtil::getFilePermissionsMode)
  }

  private fun getFilePermissionsMode(permission: PosixFilePermission): Int {
    return when (permission) {
      PosixFilePermission.OWNER_READ -> 64
      PosixFilePermission.OWNER_WRITE -> 128
      PosixFilePermission.OWNER_EXECUTE -> 256
      PosixFilePermission.GROUP_READ -> 8
      PosixFilePermission.GROUP_WRITE -> 16
      PosixFilePermission.GROUP_EXECUTE -> 32
      PosixFilePermission.OTHERS_READ -> 1
      PosixFilePermission.OTHERS_WRITE -> 2
      PosixFilePermission.OTHERS_EXECUTE -> 4
    }
  }

  /**
   * Return a set of ALL parent directories for a given path
   * This will not include path itself and not the root path
   */
  fun getParentDirs(path: String): Collection<String> {
    val parents: MutableSet<String> = mutableSetOf()
    var currentParent = getParentDir(path)
    while (currentParent != FilenameUtils.getPrefix(currentParent) && !parents.contains(currentParent)) {
      parents.add(currentParent)
      currentParent = getParentDir(currentParent)
    }
    return parents.map(FilenameUtils::separatorsToSystem)
  }

  /**
   * Returns the parent directory for a given path or `/` if there is no parent directory.
   */
  fun getParentDir(path: String): String {
    return FilenameUtils.getFullPathNoEndSeparator(path)
      .ifBlank { File.separator }
      .run(FilenameUtils::separatorsToSystem)
  }

  /**
   * Check if a path matches the given Ant pattern.
   * This ignores the directory separator.
   */
  fun matches(pattern: String, path: String): Boolean {
    return matcher.match(pattern, FilenameUtils.separatorsToUnix(normalizePath(path)))
  }
}
