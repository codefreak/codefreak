import useWorkspace, { NO_BASE_URL } from './useWorkspace'
import {
  createDirectoryPath,
  createFilePath,
  fetchWithAuthentication
} from '../../services/workspace'
import useWorkspaceBaseMutation from './useWorkspaceBaseMutation'

/**
 * A type of path
 */
export enum PathType {
  /**
   * A file
   */
  FILE,

  /**
   * A directory
   */
  DIRECTORY
}

/**
 * Provides a function to create a path in the workspace
 */
const useCreateWorkspacePathMutation = () => {
  const { baseUrl, authToken } = useWorkspace()

  return useWorkspaceBaseMutation(
    async ({ path, type }: { path: string; type: PathType }) => {
      if (baseUrl === NO_BASE_URL || authToken === undefined) {
        return Promise.reject('No base-url for the workspace given')
      }

      let fullPath

      switch (type) {
        case PathType.FILE:
          fullPath = createFilePath(baseUrl, path)
          break
        case PathType.DIRECTORY:
          fullPath = createDirectoryPath(baseUrl, path)
          break
      }

      const response = await fetchWithAuthentication(fullPath, {
        method: 'POST',
        authToken
      })

      if (!response.ok) {
        const error = await response.json()

        let message = `Could not create ${path}`

        if ('message' in error && typeof error.message === 'string') {
          message = error.message
        }

        return Promise.reject(message)
      }

      return Promise.resolve()
    }
  )
}

export default useCreateWorkspacePathMutation
