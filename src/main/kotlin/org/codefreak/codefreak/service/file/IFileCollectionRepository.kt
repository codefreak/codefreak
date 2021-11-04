package org.codefreak.codefreak.service.file

import java.util.UUID

interface IFileCollectionRepository {
  /**
   * Function to save a version to the filesystem within the backend.
   * Creates a new repository if the collectionID does not exist. Only adds a new version
   * if changes where made unless it's getting forced.
   * @param collectionID is the Id of the collection. The id is the same as the answer id
   * @param commitMessage the string representing the commit/version
   * @param force flag to force a version/save
   * @return true if the version got saved
   */
  fun saveChanges(collectionID: UUID, commitMessage: String, force: Boolean): Boolean
  /**
   * Resets (clears) the working tree and updates the files with the given Version.
   * @param collectionID is the Id of the collection. The id is the same as the answer id
   * @param changeToCommitId is the version to which should be changed
   * @param commitMessage the string representing the commit/version
   * @return true if the version got changed
   */
  fun resetAndLoadVersion(collectionID: UUID, changeToCommitId: String, commitMessage: String): Boolean
  /**
   * Function to retrieve all commit sha and their message.
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
