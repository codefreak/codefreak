package org.codefreak.codefreak.service.file

import java.util.UUID

/**
 * Provides functionality to save and load versions (commits of a git repository) and their data.
 * This interface should be used for interaction with the git repositories of the file collections.
 */
interface IFileCollectionRepository {
  /**
   * Saves a version to the filesystem within the backend.
   * Creates a new repository if the collectionID does not exist. Only adds a new version
   * if changes where made unless it's getting forced.
   * @param collectionID is the Id of the collection. The id is the same as the answer id
   * @param commitMessage the string representing the commit/version
   * @param force flag to force a version/save
   * @param requireFileSave indicates if the online ide files needs to be saved during the process
   * @return true if the version got saved
   */
  fun saveChanges(collectionID: UUID, commitMessage: String, force: Boolean, requireFileSave: Boolean = true): Boolean
  /**
   * Resets (clears) the working tree and updates the files with the given Version.
   * @param collectionID is the Id of the collection. The id is the same as the answer id
   * @param changeToCommitId is the version to which should be changed
   * @param commitMessage the string representing the commit/version
   * @return true if the version got changed
   */
  fun resetAndLoadVersion(collectionID: UUID, changeToCommitId: String, commitMessage: String): Boolean
  /**
   * Retrieves all hashes and their messages.
   * @param collectionID is the Id of the collection. The id is the same as the answer id
   * @return map of commit sha and commit message
   */
  fun getCommits(collectionID: UUID): MutableMap<String, String>
  /**
   * Function to retrieve a single commit Message by a given commitID.
   * @param collectionID is the Id of the collection. The id is the same as the answer id
   * @param commitId commit sha to retrieve the commit message from
   * @return commit message of the corresponding commit
   */
  fun getCommitMessage(collectionID: UUID, commitId: String): String
}
