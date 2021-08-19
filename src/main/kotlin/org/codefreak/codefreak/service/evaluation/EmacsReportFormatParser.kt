package org.codefreak.codefreak.service.evaluation

val EMACS_REGEX = Regex("^([^:]+?): (warning|error) ([^:]+?): \\[([^\\]]+?)\\] (.+?)\\[(\\d+)\\]\$")

class EmacsReportFormatParser
