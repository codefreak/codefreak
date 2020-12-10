package org.codefreak.codefreak.util

class InvalidCodefreakDefinitionException(message: String) : RuntimeException("Invalid ${TarUtil.CODEFREAK_DEFINITION_YML}: $message")
