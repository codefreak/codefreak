package de.code_freak.codefreak.http

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class NotFoundException(reason: String?) : ResponseStatusException(HttpStatus.NOT_FOUND, reason)
