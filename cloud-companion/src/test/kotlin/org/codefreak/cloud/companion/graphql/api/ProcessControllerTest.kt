package org.codefreak.cloud.companion.graphql.api

import java.time.Duration
import java.util.UUID
import org.codefreak.cloud.companion.ProcessManager
import org.codefreak.cloud.companion.graphql.BasicGraphqlTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier

private fun getBashPath(): String {
  if (System.getProperty("os.name").lowercase().contains("win")) {
    return "C:\\Program Files\\Git\\bin\\bash"
  }
  return "/bin/bash"
}

internal class ProcessControllerTest(
  @Autowired private val processManager: ProcessManager
) : BasicGraphqlTest() {

  @AfterEach
  fun afterEach() {
    // kill all processes after each test
    Flux.fromIterable(processManager.getProcesses())
        .flatMap { (id) -> processManager.purgeProcess(id) }
        .blockLast()
  }

  @Test
  fun `startProcess starts a new process`() {
    val id = graphQlTester.query("mutation { startProcess(cmd: [\"${getBashPath().replace("\\", "\\\\")}\"]){ id } }")
      .execute()
      .path("startProcess.id")
      .pathExists()
      .valueIsNotEmpty()
      .entity(UUID::class.java)
      .get()
    assertDoesNotThrow { processManager.getProcess(id) }
  }

  @Test
  fun `accepts additional environment variables`() {
    val id = graphQlTester.query(
      "mutation { startProcess(cmd: [\"${
        getBashPath().replace(
          "\\",
          "\\\\"
        )
      }\"], env: [\"CUSTOM=123foo123\"]){ id } }"
    )
      .execute()
      .path("startProcess.id")
      .pathExists()
      .valueIsNotEmpty()
      .entity(UUID::class.java)
      .get()
    processManager.getStdin(id).writer().let {
      it.write("echo \$CUSTOM\r\n")
      it.flush()
    }
    StepVerifier.create(processManager.getStdout(id)
      .map { it.asInputStream().readBytes().decodeToString() }
      .filter { it.contains("123foo123") }
    ).expectNextCount(1)
      .thenCancel()
      .verify(Duration.ofSeconds(3))
  }

  @Test
  fun killProcess() {
    val id = processManager.createProcess(listOf(getBashPath()))
    graphQlTester.query("mutation { killProcess(id: \"${id}\") }")
      .execute()
      .path("killProcess")
      .pathExists()
      .valueIsNotEmpty()
    // Value-matching is difficult here:
    // On *NIX Systems the exit code is 137
    // On Windows it is the int of 0xC000013A
  }

  @Test
  fun resizeProcess() {
    val id = processManager.createProcess(listOf(getBashPath()))
    graphQlTester.query("mutation { resizeProcess(id: \"${id}\", cols: 111, rows: 111) }")
      .execute()
      .path("resizeProcess")
      .pathExists()
      .valueIsNotEmpty()
    // write bash's cols and rows to stdout and wait 3 seconds for it to arrive
    processManager.getStdin(id).writer().let {
      it.write("echo $(tput cols) $(tput lines)\r\n")
      it.flush()
    }
    StepVerifier.create(processManager.getStdout(id)
      .map { it.asInputStream().readBytes().decodeToString() }
      .filter { it.contains("111 111") }
    ).expectNextCount(1)
      .thenCancel()
      .verify(Duration.ofSeconds(3))
  }

  @Test
  fun waitForProcess() {
    val id = processManager.createProcess(listOf(getBashPath()))
    val subFlux = graphQlTester.query("subscription { waitForProcess(id: \"${id}\") }")
      .executeSubscription()
      .toFlux()

    // This is here to drain stdout.
    // Otherwise, we cannot write to stdin (at least on Mac).
    processManager.getStdout(id)
      .map { it.asInputStream().readBytes().decodeToString() }
      .subscribeOn(Schedulers.boundedElastic())
      .subscribe()

    StepVerifier.create(subFlux)
      .then {
        processManager.getStdin(id).writer().let {
          it.write("exit 111\r\n")
          it.flush()
        }
      }
      .consumeNextWith { it.path("waitForProcess").matchesJson("111") }
      .expectComplete()
      .verify(Duration.ofSeconds(100))
  }
}
