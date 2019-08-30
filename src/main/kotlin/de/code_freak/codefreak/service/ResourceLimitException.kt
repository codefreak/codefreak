package de.code_freak.codefreak.service

import java.lang.RuntimeException

class ResourceLimitException(message: String? = null) : RuntimeException(message)
