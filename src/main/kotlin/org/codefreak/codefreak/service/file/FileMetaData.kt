package org.codefreak.codefreak.service.file

import java.time.Instant

/**
 * Possible type of files in a collection.
 * There is no support for symlinks as they will be pretty hard to tame.
 */
enum class FileType {
  FILE,
  DIRECTORY
}

/**
 * Contains the meta information about a file in a collection.
 */
data class FileMetaData(
  /**
   * Indicates the type of the file.
   * @see FileType
   */
  val type: FileType,

  /**
   * Absolute path of the file in the collection.
   * Always starts with a leading slash and never has a trailing slash.
   */
  val path: String,

  /**
   * Timestamp when the file has last been modified.
   */
  val lastModifiedDate: Instant,

  /**
   * Permissions of this file represented as UNIX mode.
   * On non UNIX platforms this will be null.
   */
  val mode: Int?,

  /**
   * Size of the file.
   * Will be null for non-files
   */
  val size: Long?
)
