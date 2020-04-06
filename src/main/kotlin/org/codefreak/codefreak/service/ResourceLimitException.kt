package org.codefreak.codefreak.service

import java.lang.RuntimeException

class ResourceLimitException(message: String? = null) : RuntimeException(message)
