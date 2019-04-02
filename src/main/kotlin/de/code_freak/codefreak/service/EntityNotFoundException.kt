package de.code_freak.codefreak.service

import java.lang.RuntimeException

class EntityNotFoundException(message: String? = null) : RuntimeException(message)
