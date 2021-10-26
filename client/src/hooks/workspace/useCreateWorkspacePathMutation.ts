import useWorkspace, { NO_BASE_URL } from './useWorkspace'
import { useMutation } from 'react-query'
import {
  createDirectoryPath,
  createFilePath,
  fetchWithAuthentication
} from '../../services/workspace'

export enum PathType {
  FILE,
  DIRECTORY
}

const useCreateWorkspacePathMutation = () => {
  const { baseUrl, authToken } = useWorkspace()

  return useMutation(
    async ({ path, type }: { path: string; type: PathType }) => {
      if (baseUrl === NO_BASE_URL) {
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
        const message = await response.text()
        return Promise.reject(message)
      }

      return Promise.resolve()
    }
  )
}

export default useCreateWorkspacePathMutation
