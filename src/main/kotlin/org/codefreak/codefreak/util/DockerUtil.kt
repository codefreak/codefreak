package org.codefreak.codefreak.util

import java.util.regex.Pattern

object DockerUtil {
  fun splitCommand(command: String): Array<String> {
    // from https://stackoverflow.com/a/366532/5519485
    val matchList = ArrayList<String>()
    val regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'")
    val regexMatcher = regex.matcher(command)
    while (regexMatcher.find()) {
      when {
        regexMatcher.group(1) != null -> // Add double-quoted string without the quotes
          matchList.add(regexMatcher.group(1))
        regexMatcher.group(2) != null -> // Add single-quoted string without the quotes
          matchList.add(regexMatcher.group(2))
        else -> // Add unquoted word
          matchList.add(regexMatcher.group())
      }
    }
    return matchList.toArray(arrayOf())
  }

  /**
   * Inherit behaviour of the standard Docker CLI and fallback to :latest if no tag is given
   */
  fun normalizeImageName(imageName: String) =
      if (imageName.contains(':')) {
        imageName
      } else {
        "$imageName:latest"
      }
}
