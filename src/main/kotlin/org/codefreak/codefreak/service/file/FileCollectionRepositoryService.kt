package org.codefreak.codefreak.service.file

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import org.apache.tomcat.util.http.fileupload.FileUtils
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.IdeService
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.InitCommand
import org.eclipse.jgit.api.errors.CanceledException
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FileCollectionRepositoryService(val config: AppConfiguration, val answerService: AnswerService, val ideService: IdeService) : IFileCollectionRepository {

  override fun saveChanges(collectionID: UUID, commitMessage: String, force: Boolean, requireFileSave: Boolean): Boolean {
    if (requireFileSave) {
      val answer = answerService.findAnswer(collectionID)
      ideService.saveAnswerFiles(answer)
    }
    val workingTreePath = createCollectionPath(collectionID)
    val gitDir = getGitDir(collectionID)
    if (!Files.exists(Paths.get(gitDir))) {
      val git = InitCommand()
        .setDirectory(workingTreePath.toFile())
        .setGitDir(File(gitDir))
        .call()
      git.add().addFilepattern(".").call()
      git.commit().setMessage(commitMessage).call()
      setCurrentHeadCommitId(git, collectionID)
      return true
    } else {
      // fetch git repository from .git in folder
      val git = Git.open(File(gitDir))
      if (gotTouched(git) || force == true) {
        val missingFiles = git.status().call().missing
        for (remove in missingFiles) {
          git.rm().addFilepattern(remove).call()
        }
        val addedFiles = git.status().call().added
        for (add in addedFiles) {
          git.add().addFilepattern(add).call()
        }
        git.add().addFilepattern(".").call()
        git.commit().setMessage(commitMessage).call()
        setCurrentHeadCommitId(git, collectionID)
        updateIde(collectionID)
        return true
      }
      return false
    }
  }

  private fun updateIde(collectionID: UUID) {
    ideService.answerFilesUpdatedExternally(collectionID)
  }

  /**
   * Function which checks if any changes where made to the version.
   * @param git the Jgit git instance to operate on
   * @return true if any changes where made
   */
  private fun gotTouched(git: Git): Boolean {
    // source: https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ListUncommittedChanges.java
    val status = git.status().call()

    val added = status.added
    if (added.isNotEmpty()) {
      return true
    }

    val changed = status.changed
    if (changed.isNotEmpty()) {
      return true
    }

    val missing = status.missing
    if (missing.isNotEmpty()) {
      return true
    }

    val modified = status.modified
    if (modified.isNotEmpty()) {
      return true
    }

    val untracked = status.untracked
    if (untracked.isNotEmpty()) {
      return true
    }

    val untrackedFolders = status.untrackedFolders
    if (untrackedFolders.isNotEmpty()) {
      return true
    }
    return false
  }

  /**
   * Function which checks if any changes where made to the version.
   * This function exists because a different handling/check is needed for a
   * version change.
   * @param git the Jgit git instance to operate on
   * @return true if any changes where made
   */
  private fun gotTouchedForReset(git: Git): Boolean {
    // source: https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ListUncommittedChanges.java
    val status = git.status().call()

    val missing = status.missing
    if (missing.isNotEmpty()) {
      return true
    }

    val modified = status.modified
    if (modified.isNotEmpty()) {
      return true
    }

    val untracked = status.untracked
    if (untracked.isNotEmpty()) {
      return true
    }

    val untrackedFolders = status.untrackedFolders
    if (untrackedFolders.isNotEmpty()) {
      return true
    }
    return false
  }

  /**
   * Saves the current used version (git sha) to the answer table in the database.
   * @param git the Jgit git instance to operate on
   * @param collectionID is the Id of the collection. The id is the same as the answer id
   */
  private fun setCurrentHeadCommitId(git: Git, collectionID: UUID) {
    val latestCommit = Git(git.repository).log().setMaxCount(1).call().iterator().next()
    answerService.saveCommitId(collectionID, latestCommit.name)
  }

  override fun resetAndLoadVersion(collectionID: UUID, changeToCommitId: String, commitMessage: String): Boolean {
    val answer = answerService.findAnswer(collectionID)
    ideService.saveAnswerFiles(answer)
    val workingTreePath = createCollectionPath(collectionID)
    try {
      val git = getGit(collectionID)
      val commitId = answerService.getCommitId(collectionID)
      if (commitId != changeToCommitId) {
        if (gotTouchedForReset(git)) {
          saveChanges(collectionID, commitMessage, true, false)
        }
        FileUtils.cleanDirectory(workingTreePath.toFile())
        git.checkout().setStartPoint(changeToCommitId).setAllPaths(true).call()
        val missingFiles = git.status().call().missing
        for (remove in missingFiles) {
          git.rm().addFilepattern(remove).call()
        }
        git.add().addFilepattern(".").call()
        answerService.saveCommitId(collectionID, changeToCommitId)
        updateIde(collectionID)
        return true
      }
      return false
    } catch (err: CanceledException) {
      return false
    }
  }

  override fun getCommits(collectionID: UUID): MutableMap<String, String> {
    val commitMap: MutableMap<String, String> = mutableMapOf()
    try {
      val git = getGit(collectionID)
      val branches: List<Ref> = git.branchList().call()
      for (branch in branches) {
        if (branch.getName().equals("refs/heads/master")) {
          val commits = git.log().add(branch.getObjectId()).call()
          for (commit in commits) {
            commitMap.put(commit.name, commit.shortMessage)
          }
        }
      }
      return commitMap
    } catch (err: CanceledException) {
      return commitMap
    }
  }

  override fun getCommitMessage(collectionID: UUID, commitId: String): String {
    try {
      val git = getGit(collectionID)
      return getRev(git, commitId).shortMessage
    } catch (err: CanceledException) {
      return ""
    }
  }

  /**
   * @returns RevCommit object, which contains information about the commit
   * @param git is the instance from which the commit shall be fetched
   * @param commitId sha id of the commit to be fetched
   */
  private fun getRev(git: Git, commitId: String): RevCommit {
    val commitObjectId = ObjectId.fromString(commitId)
    RevWalk(git.repository).use { revWalk -> return revWalk.parseCommit(commitObjectId) }
  }

  /**
   * @returns Get instance of Jgit
   * @param collectionID the id which specifies, which git repository shall be loaded
   */
  private fun getGit(collectionID: UUID): Git {
    val gitDir = getGitDir(collectionID)
    if (!Files.exists(Paths.get(gitDir))) {
      val log = LoggerFactory.getLogger(this::class.java)
      log.error("The path \"$gitDir\" could not be found")
      throw CanceledException("Path not found")
    }
    return Git.open(File(gitDir))
  }

  /**
   * @returns the git path corresponding to the collection
   * @param collectionID the id of the collection the path shall be returned for
   */
  private fun getGitDir(collectionID: UUID): String {
    return config.files.fileSystem.gitVersioningCollectionStoragePath + "/$collectionID/"
  }

  /**
   * Creates the directory path for the collection or simply returns it if it already exists.
   */
  private fun createCollectionPath(collectionId: UUID): Path {
    val collectionPath = getCollectionPath(collectionId)
    return Files.createDirectories(collectionPath)
  }

  private fun getCollectionPath(collectionId: UUID): Path {
    val basePath = config.files.fileSystem.collectionStoragePath
    return Paths.get(basePath, collectionId.toString())
  }
}
