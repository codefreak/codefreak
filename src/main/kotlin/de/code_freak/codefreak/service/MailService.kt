package de.code_freak.codefreak.service

interface MailService {
  fun send(to: String, subject: String, content: String)
}
