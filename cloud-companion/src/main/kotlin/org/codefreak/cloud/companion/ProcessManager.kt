package org.codefreak.cloud.companion

import com.pty4j.PtyProcessBuilder
import java.io.OutputStream
import java.util.UUID
import kotlin.io.path.absolutePathString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class ProcessManager {
  private val processMap: MutableMap<UUID, Process> = mutableMapOf()
  private val outputStreamCache: MutableMap<UUID, Flux<DataBuffer>> = mutableMapOf()

  @Autowired
  private lateinit var fileService: FileService

  fun createProcess(cmd: List<String>, additionalEnv: Map<String, String> = emptyMap()): UUID {
    var uid: UUID
    // handle rare UUID collisions properly instead of overwriting it
    do {
      uid = UUID.randomUUID()
    } while (processMap.putIfAbsent(uid, generateProcess(cmd, additionalEnv)) != null)
    return uid
  }

  fun getProcesses(): List<Pair<UUID, Process>> {
    return processMap.entries.map { (k, v) -> Pair(k, v) }
  }

  fun purgeProcess(id: UUID): Mono<Process> {
    return Mono.justOrEmpty(processMap.remove(id))
      .flatMap { process ->
        process.destroyForcibly()
        process.waitForMono().map { process }
      }.map {
        outputStreamCache.remove(id)
        it
      }
  }

  fun getProcess(uid: UUID): Process {
    return processMap[uid] ?: throw IllegalArgumentException("There is no process $uid")
  }

  fun getStdout(uid: UUID): Flux<DataBuffer> {
    return outputStreamCache.computeIfAbsent(uid) {
      getProcess(uid)
        .getInputStreamFlux()
        .subscribeOn(Schedulers.boundedElastic())
        .cache()
    }
  }

  fun getStdin(uid: UUID): OutputStream {
    return getProcess(uid).outputStream
  }

  private fun generateProcess(cmd: List<String>, env: Map<String, String>): Process {
    return PtyProcessBuilder(cmd.toTypedArray())
      .setDirectory(fileService.resolve("/").absolutePathString())
      .setEnvironment(getEnvironment(env))
      // redirect stderr to stdout, so we only have to subscribe to one stream
      .setRedirectErrorStream(true)
      .start()
  }

  /**
   * Inherit the parent environment variables but remove kubernetes master discovery variables
   * as they cannot be removed with enableServiceLinks=false.
   */
  private fun getEnvironment(additionalEnv: Map<String, String>): Map<String, String> {
    val defaultEnv = System.getenv()
      .filterKeys {
        !it.startsWith("KUBERNETES_")
      }.toMutableMap()
    defaultEnv.putAll(additionalEnv)
    // Make the terminal non-color by default but this can be overridden via additionalEnv
    defaultEnv.putIfAbsent("TERM", "xterm")
    return defaultEnv
  }
}
