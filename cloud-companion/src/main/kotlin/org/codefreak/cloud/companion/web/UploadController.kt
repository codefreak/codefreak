package org.codefreak.cloud.companion.web

import org.codefreak.cloud.companion.FileService
import org.codefreak.cloud.companion.FileServiceException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/upload")
class UploadController {
  companion object {
    private val log = LoggerFactory.getLogger(UploadController::class.java)
  }

  @Autowired
  private lateinit var fileService: FileService

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun uploadFiles(@RequestPart files: Flux<Part>): Mono<Void> {
    return files
        .flatMap {
          if (it !is FilePart) {
            Flux.error(
                ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "'files' form data should be a file upload with a proper filename specified"
                )
            )
          } else {
            log.debug("Saving uploaded file ${it.filename()}")
            fileService.saveUpload(it)
          }
        }
        .doOnError(FileServiceException::class.java) { e ->
          throw ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              e.message
          )
        }
        .then()
  }
}
