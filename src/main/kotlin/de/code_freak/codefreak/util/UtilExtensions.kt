package de.code_freak.codefreak.util

fun String.withTrailingSlash(): String = if (endsWith("/")) this else "$this/"
