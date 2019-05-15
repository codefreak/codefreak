package de.code_freak.codefreak.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LogMailService : MailService {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun send(to: String, subject: String, content: String) {
    log.info("Will send '$subject' to $to:\n${content.prependIndent(">    ")}")
  }
}
