package org.codefreak.cloud.companion.web

import org.codefreak.cloud.companion.FileService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/files-tar")
class FilesTarController {
  companion object {
    private val log = LoggerFactory.getLogger(FilesTarController::class.java)
  }

  @Autowired
  private lateinit var fileService: FileService

  @PostMapping(consumes = ["application/x-tar"])
  @ResponseStatus(HttpStatus.CREATED)
  fun uploadArchive(exchange: ServerWebExchange): Mono<Void> {
    return fileService.overrideFilesByTar(exchange.request.body).then()
  }

  /**
   * Download a tar archive containing all project files.
   * Optional parameter "filter" allows to specify an ant-style path pattern so the archive will
   * only contain files/directories matching the pattern.
   */
  @GetMapping(produces = ["application/x-tar"])
  fun downloadArchive(response: ServerHttpResponse, @RequestParam filter: String? = null): Mono<Void> {
    response.headers["Content-Disposition"] = "filename=\"files.tar\""
    // ensure pattern has a leading slash because our project-paths always start with a leading slash
    return response.writeWith(fileService.createTar(response.bufferFactory(), filter?.trimStart('/')?.let { "/$it" }))
  }
}
