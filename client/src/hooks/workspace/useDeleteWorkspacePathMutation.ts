import useWorkspace, { NO_BASE_URL } from './useWorkspace'
import { useMutation } from 'react-query'
import { deletePath, fetchWithAuthentication } from '../../services/workspace'

/**
 * Provides a function to delete a path from the workspace
 */
const useDeleteWorkspacePathMutation = () => {
  const { baseUrl, authToken } = useWorkspace()

  return useMutation(async ({ path }: { path: string }) => {
    if (baseUrl === NO_BASE_URL) {
      return Promise.reject('No base-url for the workspace given')
    }

    const fullPath = deletePath(baseUrl, path)

    const response = await fetchWithAuthentication(fullPath, {
      method: 'DELETE',
      authToken
    })

    if (!response.ok) {
      const message = await response.text()
      return Promise.reject(message)
    }

    return Promise.resolve()
  })
}

export default useDeleteWorkspacePathMutation
