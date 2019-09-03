package de.code_freak.codefreak.service

import de.code_freak.codefreak.config.AppConfiguration
import de.code_freak.codefreak.entity.Answer
import org.eclipse.jgit.api.Git
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.io.OutputStream
import java.net.URI
import java.nio.file.Files

@Service
@ConditionalOnProperty("code-freak.git-import.enabled")
class GitService {
  @Autowired
  lateinit var configuration: AppConfiguration

  @Autowired
  lateinit var answerService: AnswerService

  fun getSupportedHosts() = configuration.gitImport.remotes.map { remote -> remote.host }

  fun getRemote(host: String) = configuration.gitImport.remotes.find { remote -> remote.host == host }

  fun importFiles(remoteUrlString: String, answer: Answer) {
    val uri = URI(remoteUrlString)
    val remote = getRemote(uri.host) ?: throw IllegalArgumentException("Import from '${uri.host}' is not supported.")
    val gitUri = getGitUri(remote, uri)
    answerService.setFiles(answer.id).use {
      createRemoteTarArchive(gitUri, it)
    }
  }

  fun createRemoteTarArchive(remoteUri: URI, output: OutputStream) {
    // InMemoryRepository does not work because it will not load the ssh keys from the local filesystem
    val tmpDir = Files.createTempDirectory("CodeFreakGitClone").toFile()
    val git = Git.cloneRepository()
        .setURI(remoteUri.toString())
        .setDirectory(tmpDir)
        .call()
    git.archive()
        .setTree(git.repository.resolve("master"))
        .setFormat("tar")
        .setOutputStream(output)
        .call()
    tmpDir.deleteRecursively()
  }

  private fun getGitUri(remoteConfig: AppConfiguration.GitImport.GitRemote, inputUrl: URI): URI {
    val uriBuilder = UriComponentsBuilder.fromUriString(remoteConfig.sshBaseUrl).replacePath(inputUrl.path)
    return uriBuilder.build().toUri()
  }
}
