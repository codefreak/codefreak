package de.code_freak.codefreak.config

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.eclipse.jgit.archive.ArchiveFormats
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.util.FS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty("code-freak.git-import.enabled")
class GitImportConfiguration {
  @Autowired
  lateinit var configuration: AppConfiguration

  init {
    ArchiveFormats.registerAll()
  }

  @Bean
  fun sshSessionFactory(): SshSessionFactory {
    val factory = object : JschConfigSessionFactory() {
      override fun configure(hc: OpenSshConfig.Host?, session: Session?) {
        // not needed but has to be overridden
      }

      override fun createDefaultJSch(fs: FS?): JSch {
        val jsch = super.createDefaultJSch(fs)
        configuration.gitImport.remotes.forEach {
          jsch.addIdentity(it.sshKey, it.sshKeyPass)
        }
        return jsch
      }
    }
    SshSessionFactory.setInstance(factory)
    return factory
  }
}
