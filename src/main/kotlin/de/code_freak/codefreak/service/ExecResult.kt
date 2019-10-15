package de.code_freak.codefreak.service

data class ExecResult(val exitCode: Long, val stdout: String, val stderr: String = "") {
  constructor() : this("", -1)
  constructor(output: String, exitCode: Long): this(exitCode, output)
  val output = stdout
  val success = exitCode == 0L
}
