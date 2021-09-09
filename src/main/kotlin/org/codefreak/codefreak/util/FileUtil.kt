package org.codefreak.codefreak.util

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import org.apache.commons.io.FilenameUtils

object FileUtil {

  /**
   * Remove leading dots and slashes from given path and normalizes patterns like `foo/../bar` to `bar`.
   * The returned value has no leading or trailing path separator.
   * Expected results are very similar to FilenameUtils.normalize, except the "no parent directory left" case:
   * ../a will return a
   * /a/../../b/c will return b/c
   */
  fun sanitizePath(vararg name: String): String {
    var concated = name.joinToString(File.separator)
    // FilenameUtils.normalize returns null in case it has no parent directory left to work with
    // so "/../foo" will return null. We trick this by prepending fake directories to the original path
    // until we get a valid path from FilenameUtils.normalize.
    while (FilenameUtils.normalize(concated) === null) {
      concated = "a" + File.separatorChar + concated
    }
    return FilenameUtils.normalizeNoEndSeparator(concated).trimStart(File.separatorChar)
  }

  fun getFilePermissionsMode(permissions: Set<PosixFilePermission>): Int {
    return permissions.sumOf(FileUtil::getFilePermissionsMode)
  }

  fun getFilePermissionsMode(permission: PosixFilePermission): Int {
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
   * This will not include path itself and not the root path /
   */
  fun getParentDirs(path: String): Set<String> {
    val parents: MutableSet<String> = mutableSetOf()
    var currentParent: Path? = Paths.get("/$path").parent
    while (currentParent != null && currentParent.toString() != "/" && !parents.contains(currentParent.toString())) {
      parents.add(currentParent.toString())
      currentParent = currentParent.parent
    }
    return parents
  }

  /**
   * Returns the parent directory for a given path or `/` if there is no parent directory.
   */
  fun getParentDir(path: String): String {
    return Paths.get("/" + TarUtil.normalizeFileName(path)).parent?.toString() ?: "/"
  }
}
