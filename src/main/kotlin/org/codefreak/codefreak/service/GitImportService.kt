package org.codefreak.codefreak.service

import java.io.OutputStream
import java.net.URI
import java.nio.file.Files
import org.codefreak.codefreak.config.AppConfiguration
import org.eclipse.jgit.api.Git
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

@Service
class GitImportService : BaseService() {
  @Autowired
  lateinit var configuration: AppConfiguration

  @Autowired
  lateinit var answerService: AnswerService

  fun getRemote(host: String) = configuration.gitImport.remotes.find { remote -> remote.host == host }

  /**
   * @param out is not closed
   */
  fun importFiles(remoteUrlString: String, out: OutputStream) {
    val uri = try {
      URI(remoteUrlString).let { it.host.length; it } // check for null
    } catch (e: Exception) {
      throw IllegalArgumentException("Invalid URL")
    }
    val remote = getRemote(uri.host) ?: throw IllegalArgumentException("Import from '${uri.host}' is not supported.")
    val gitUri = getGitUri(remote, uri)
    createRemoteTarArchive(gitUri, out)
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
