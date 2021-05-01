package org.codefreak.codefreak.util

import java.nio.file.attribute.PosixFilePermission

object FileUtil {
  private val stripPrefixPattern = """^(?:\.*/)*(?:\.+$)?""".toRegex()

  /**
   * Remove leading dots and slashes from given path
   */
  fun normalizeName(name: String) = name.trim().trim('/').replace(stripPrefixPattern, "").trim()

  fun getFilePermissionsMode(permissions: Set<PosixFilePermission>): Int {
    return permissions.map {
      getFilePermissionsMode(it)
    }.reduce { result, mode -> result + mode }
  }

  fun getFilePermissionsMode(permission: PosixFilePermission): Int {
    return when (permission) {
      PosixFilePermission.OWNER_READ -> 100
      PosixFilePermission.OWNER_WRITE -> 200
      PosixFilePermission.OWNER_EXECUTE -> 400
      PosixFilePermission.GROUP_READ -> 10
      PosixFilePermission.GROUP_WRITE -> 20
      PosixFilePermission.GROUP_EXECUTE -> 40
      PosixFilePermission.OTHERS_READ -> 1
      PosixFilePermission.OTHERS_WRITE -> 2
      PosixFilePermission.OTHERS_EXECUTE -> 4
    }
  }
}
