package org.codefreak.cloud.companion.graphql.model

import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent

fun FileSystemEventType(kind: WatchEvent.Kind<*>) = when (kind) {
  StandardWatchEventKinds.ENTRY_CREATE -> FileSystemEventType.CREATED
  StandardWatchEventKinds.ENTRY_DELETE -> FileSystemEventType.DELETED
  StandardWatchEventKinds.ENTRY_MODIFY -> FileSystemEventType.MODIFIED
  else -> FileSystemEventType.UNKNOWN
}

enum class FileSystemEventType {
  CREATED, DELETED, MODIFIED, UNKNOWN
}
