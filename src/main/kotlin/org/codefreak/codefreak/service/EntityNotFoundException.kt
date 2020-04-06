package org.codefreak.codefreak.service

import java.lang.RuntimeException

class EntityNotFoundException(message: String? = null) : RuntimeException(message)
