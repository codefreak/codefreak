package org.codefreak.codefreak.service.workspace

import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit.SECONDS
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Manager that keeps track of running workspaces and shuts down IDE workspaces that
 * have been idle for too long.
 */
@Component
class WorkspaceLifecycleManager(
  /**
   * Timeout in seconds after which idle workspace will be removed
   */
  @Value("#{@config.workspaces.maxIdleThreshold}")
  private val removeTimeout: Long = 60 * 5L,
  private val workspaceService: WorkspaceService,
  private val clientService: WorkspaceClientService
) {
  companion object {
    private val log = LoggerFactory.getLogger(WorkspaceLifecycleManager::class.java)
  }

  var clock: Clock = Clock.systemDefaultZone()

  /**
   * Registry that holds a time reference of workspace-reference to time when the workspace
   * has been encountered as idle the first time.
   */
  private var idleWorkspaceStore = mapOf<String, Instant>()

  /**
   * List of workspace purposes that are watched. Only workspaces that are in user-control
   * should be shut down.
   */
  private val watchedPurposes = listOf(
    WorkspacePurpose.ANSWER_IDE,
    WorkspacePurpose.TASK_IDE
  )

  @Scheduled(
    fixedRateString = "#{@config.workspaces.idleCheckInterval}",
    initialDelayString = "#{@config.workspaces.idleCheckInterval}",
    timeUnit = SECONDS
  )
  fun removeIdleWorkspaces() {
    // keep a fixed "now" for further processing to ignore processing times
    val now = clock.instant()
    val removeInstant = now.minusSeconds(removeTimeout)
    log.debug("Removing workspace(s) that have been idle since $removeInstant (limit is ${removeTimeout}s)")
    val idleWorkspaces = workspaceService.findAllWorkspaces()
      .filter { watchedPurposes.contains(it.identifier.purpose) }
      .mapNotNull { reference ->
        val client = clientService.getClient(reference)
        val numWsConnections = try {
          client.countWebsocketConnections()
        } catch (e: IllegalStateException) {
          // ignore workspaces that are in an erroneous state (maybe just started or shutting down)
          log.debug("Workspace $reference appears to be broken. Ignoring for idle check.")
          return@mapNotNull null
        }
        if (numWsConnections <= 0) {
          // either take their existing idle time or mark them idle since now
          val idleSince = idleWorkspaceStore[reference.identifier.hashString()] ?: now
          Pair(reference.identifier, idleSince)
        } else {
          // workspace is not idle (anymore)
          null
        }
      }
      .toMap()
      .filter { (identifier, idleSince) ->
        val idleFor = ChronoUnit.SECONDS.between(idleSince, now)
        val timeLeft = ChronoUnit.SECONDS.between(removeInstant, idleSince)
        if (timeLeft <= 0) {
          log.info("Shutting down workspace ${identifier.hashString()} that has been idle for ${idleFor}s")
          workspaceService.deleteWorkspace(identifier)
          false
        } else {
          log.debug("Workspace ${identifier.hashString()} has been idle for ${idleFor}s. Will be removed in ${timeLeft}s")
          true
        }
      }

    // store all workspaces that have not been removed this round for the next cycle
    idleWorkspaceStore = idleWorkspaces.mapKeys { it.key.hashString() }
  }
}
