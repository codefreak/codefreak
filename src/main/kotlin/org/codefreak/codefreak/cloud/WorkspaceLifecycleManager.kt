package org.codefreak.codefreak.cloud

import java.time.Instant
import java.time.temporal.ChronoUnit
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WorkspaceLifecycleManager(
  private val workspaceService: WorkspaceService,
  private val wsClientFactory: WorkspaceClientFactory
) {
  companion object {
    private val log = LoggerFactory.getLogger(WorkspaceLifecycleManager::class.java)
  }

  /**
   * Registry that holds a time reference of workspace-reference to time when the workspace
   * has been encountered as idle the first time.
   */
  private var idleLedger = mapOf<String, Instant>()

  /**
   * Timeout in s after which the workspace will be removed
   */
  private val removeTimeout = 60 * 5L

  @Scheduled(
    fixedRateString = "\${codefreak.ide.idle-check-rate}",
    initialDelayString = "\${codefreak.ide.idle-check-rate}"
  )
  fun removeIdleWorkspaces() {
    // keep a fixed "now" for further processing to ignore processing times
    val now = Instant.now()
    val allWorkspaces = workspaceService.findAllWorkspaces().filter {
      // ignore evaluation workspaces
      it.id.purpose != WorkspacePurpose.EVALUATION
    }
    log.debug("Checking ${allWorkspaces.count()} workspace(s) for idle (limit is ${removeTimeout}s)")

    val allIdleWorkspaces: Map<RemoteWorkspaceReference, Instant> = allWorkspaces.mapNotNull { reference ->
      val client = wsClientFactory.createClient(reference)
      val numWsConnections = try {
        client.countWebsocketConnections()
      } catch (e: IllegalStateException) {
        // ignore workspaces that are in an erroneous state (maybe just started or shutting down)
        log.debug("Workspace $reference appears to be broken. Ignoring for idle check.")
        return@mapNotNull null
      }
      if (numWsConnections <= 0) {
        val idleSince = idleLedger[reference.id.hashString()] ?: now
        Pair(reference, idleSince)
      } else {
        null
      }
    }.toMap()
    if (allIdleWorkspaces.isNotEmpty()) {
      log.debug("Watching ${allIdleWorkspaces.size} workspaces that have been identified as idle")
    }

    // remove workspaces that have been idle for too long
    val removeInstant = now.minusSeconds(removeTimeout)
    val remainingIdleWorkspaces = allIdleWorkspaces.mapNotNull { (reference, idleSince) ->
      val idleFor = ChronoUnit.SECONDS.between(idleSince, now)
      val timeLeft = ChronoUnit.SECONDS.between(removeInstant, idleSince)
      if (timeLeft <= 0) {
        log.info("Shutting down workspace ${reference.id.hashString()} that has been idle for ${idleFor}s")
        workspaceService.deleteWorkspace(reference.id)
        null
      } else {
        log.debug("Workspace ${reference.id.hashString()} has been idle for ${idleFor}s. Will be removed in ${timeLeft}s")
        Pair(reference.id.hashString(), idleSince)
      }
    }.toMap()

    // store all workspaces that have not been removed this round for the next cycle
    idleLedger = remainingIdleWorkspaces
  }
}
