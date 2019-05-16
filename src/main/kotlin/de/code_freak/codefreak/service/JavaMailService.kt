package de.code_freak.codefreak.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Primary
@Service
@ConditionalOnProperty(prefix = "spring.mail", name = ["host"])
class JavaMailService : BaseService(), MailService {

  @Autowired
  lateinit var mailSender: JavaMailSender

  override fun send(to: String, subject: String, content: String) {
    val mail = mailSender.createMimeMessage()
    val mailHelper = MimeMessageHelper(mail)
    mailHelper.setTo(to)
    mailHelper.setFrom("code-freak@noreply.fh-kiel.de")
    mailHelper.setSubject("Your registration on Code FREAK")
    mailHelper.setText(content)
    mailSender.send(mail)
  }
}
