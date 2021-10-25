package org.codefreak.codefreak.util

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.codefreak.codefreak.entity.Task

object TaskUtil {
  fun Task.isHidden(entry: TarArchiveEntry): Boolean {
    hiddenFiles.plus("codefreak.yml").forEach {
      if (FileUtil.matches(it, entry.name)) return true
    }
    return false
  }

  fun Task.isProtected(entry: TarArchiveEntry): Boolean {
    protectedFiles.forEach {
      if (FileUtil.matches(it, entry.name)) return true
    }
    return false
  }
}
