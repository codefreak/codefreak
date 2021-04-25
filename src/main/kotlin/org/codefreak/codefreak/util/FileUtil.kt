package org.codefreak.codefreak.util

object FileUtil {
  private val stripPrefixPattern = """^(?:\.*/)*(?:\.+$)?""".toRegex()

  /**
   * Remove leading dots and slashes from given path
   */
  fun normalizeName(name: String) = name.trim().trim('/').replace(stripPrefixPattern, "").trim()
}
