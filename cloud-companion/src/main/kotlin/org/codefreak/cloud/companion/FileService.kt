package org.codefreak.cloud.companion

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.util.AntPathMatcher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.File
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.FileSystem
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isSameFileAs

private val DEFAULT_WATCH_EVENT_KINDS = arrayOf(
  StandardWatchEventKinds.ENTRY_CREATE,
  StandardWatchEventKinds.ENTRY_MODIFY,
  StandardWatchEventKinds.ENTRY_DELETE
)

class PosixTarArchiveOutputStream(out: OutputStream) : TarArchiveOutputStream(out) {
  init {
    setLongFileMode(LONGFILE_POSIX)
    setBigNumberMode(BIGNUMBER_STAR)
  }
}

@Service
class FileService(
  @Autowired private val tika: Tika,
  @Value("#{config.projectFilesPath}") basePathString: String
) {
  private val basePath = Paths.get(basePathString)
  val fileSystem: FileSystem = basePath.fileSystem

  private val antPathMatcher = AntPathMatcher()

  init {
    if (!basePath.exists()) {
      basePath.createDirectories()
    }
  }

  fun resolve(path: String): Path {
    return basePath.resolve(normalizePath(path))
  }

  fun relativePath(path: Path): String {
    return "/${basePath.relativize(path)}"
  }

  /**
   * Watch a directory for events using nio's WatchService.
   * This creates a Flux which emits new WatchEvents.
   * The WatchService will be closed when the subscription ends.
   *
   * Warning: This seems to be quite slow on MacOS because there is no
   * native watching implementation by default. Changes are detected by polling instead:
   * https://stackoverflow.com/a/11182515/1526257.
   * So notifications for changes may take some time if you are running this on MacOS.
   */
  fun watchDirectory(directory: Path): Flux<WatchEvent<*>> {
    if (!directory.isDirectory()) {
      return Flux.error(IllegalArgumentException("$directory is not a directory"))
    }

    return Mono.fromCallable {
      fileSystem.newWatchService().also {
        directory.register(it, DEFAULT_WATCH_EVENT_KINDS)
      }
    }.flatMapMany { watchService ->
      Flux.generate<WatchKey> { it.next(watchService.take()) }
        .subscribeOn(Schedulers.boundedElastic())
        .doOnCancel { watchService.close() }
        .doOnDiscard(WatchKey::class.java) { it.reset() }
        .flatMap {
          it.reset()
          Flux.fromIterable(it.pollEvents())
        }
    }
  }

  fun saveUpload(file: FilePart): Mono<Void> {
    return Mono.just(file)
      .flatMap { part ->
        withParentPathExists(part.filename()) {
          part.transferTo(it)
        }
      }.then()
  }

  fun <T> withParentPathExists(filePath: String, consume: (it: Path) -> T): T {
    val path = resolve(filePath)
    if (path.parent != null && !path.parent.exists()) {
      try {
        Files.createDirectories(path.parent)
      } catch (e: FileSystemException) {
        throw FileServiceException("Could not create parent dirs of $path: ${e.message}")
      }
    }
    return consume(path)
  }

  fun createTar(bufferFactory: DataBufferFactory, filterPattern: String?) = createTar(bufferFactory) {
    filterPattern == null || antPathMatcher.match(filterPattern, relativePath(it))
  }

  fun createTar(
    bufferFactory: DataBufferFactory,
    prerequisite: (file: Path) -> Boolean = { true }
  ): Flux<DataBuffer> {
    return Flux.create { sink ->
      val buffer = bufferFactory.allocateBuffer()
      buffer.asOutputStream().use { bufferStream ->
        val outputStream = PosixTarArchiveOutputStream(bufferStream)
        Files.walk(resolve("/"))
          // exclude root directory
          .filter { !it.isSameFileAs(basePath) }
          .filter(prerequisite)
          // TODO: symlinks are created as regular files
          .forEach { file ->
            val entry = outputStream.createArchiveEntry(file, relativePath(file).trimStart('/'))
            outputStream.putArchiveEntry(entry)
            if (!file.isDirectory()) {
              IOUtils.copy(file.toFile(), outputStream)
            }
            // TODO: we could emit a new data buffer after every entry instead of the full archive
            outputStream.closeArchiveEntry()
          }
        outputStream.finish()
      }
      sink.next(buffer)
      sink.complete()
    }
  }

  fun overrideFilesByTar(tarArchive: Flux<DataBuffer>): Mono<Void> {
    return tarFluxFromDataBuffer(tarArchive)
      .doFirst { purgeFiles() }
      .map { (archiveStream, entry) -> extractEntryToFiles(archiveStream, entry) }
      .then()
  }

  fun purgeFiles() {
    FileUtils.cleanDirectory(resolve("/").toFile())
  }

  private fun extractEntryToFiles(archiveInputStream: TarArchiveInputStream, entry: TarArchiveEntry) {
    if (entry.isDirectory) {
      Files.createDirectories(resolve(entry.name))
    } else {
      withParentPathExists(entry.name) {
        Files.newOutputStream(it).use { IOUtils.copy(archiveInputStream, it) }
      }
    }
  }

  /**
   * Get the mime type for a file that should be used when delivering
   * a file to the browser
   */
  fun getDownloadMimeType(path: Path): MediaType {
    val mime = tika.detect(path)
    return when {
      // allow to display images natively in browsers
      mime.startsWith("image/") -> MediaType.parseMediaType(mime)
      // Do never expose real mime values for text or this will allow to deliver executable JS for example.
      // The utf-8 encoding might not be correct but most browsers will complain (in the console) if
      // they do not receive any text encoding.
      mime.startsWith("text/") -> MediaType.parseMediaType("text/plain;charset=utf-8")
      // force download for everything else
      else -> MediaType.APPLICATION_OCTET_STREAM
    }
  }

  private fun tarFluxFromDataBuffer(dataBuffers: Flux<DataBuffer>): Flux<Pair<TarArchiveInputStream, TarArchiveEntry>> {
    return Flux.from {
      val outputStream = PipedOutputStream()
      val archiveStream = TarArchiveInputStream(PipedInputStream(outputStream))

      val reader = DataBufferUtils.write(dataBuffers, outputStream)
        .doFinally { outputStream.close() }
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(DataBufferUtils.releaseConsumer())

      Flux.generate<Pair<TarArchiveInputStream, TarArchiveEntry>> { sink ->
        val next = archiveStream.nextTarEntry
        if (next != null) {
          sink.next(Pair(archiveStream, next))
        } else {
          sink.complete()
        }
      }
        .doFinally { reader.dispose() }
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(it)
    }
  }

  /**
   * Ensures path has correct file separator and does not escape any directory.
   */
  private fun normalizePath(vararg parts: String): String {
    val joinedParts = parts.joinToString(separator = File.separator)
    return FilenameUtils.normalizeNoEndSeparator(joinedParts)?.trim(File.separatorChar)
        ?: throw IllegalArgumentException("Invalid path specified: $joinedParts")
  }
}
