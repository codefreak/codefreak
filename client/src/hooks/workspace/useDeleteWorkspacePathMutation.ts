import useWorkspace, { NO_BASE_URL } from './useWorkspace'
import { deletePath, fetchWithAuthentication } from '../../services/workspace'
import useWorkspaceBaseMutation from './useWorkspaceBaseMutation'

/**
 * Provides a function to delete a path from the workspace
 */
const useDeleteWorkspacePathMutation = () => {
  const { baseUrl, authToken } = useWorkspace()

  return useWorkspaceBaseMutation(async ({ path }: { path: string }) => {
    if (baseUrl === NO_BASE_URL || authToken === undefined) {
      return Promise.reject('No base-url for the workspace given')
    }

    const fullPath = deletePath(baseUrl, path)

    const response = await fetchWithAuthentication(fullPath, {
      method: 'DELETE',
      authToken
    })

    if (!response.ok) {
      const error = await response.json()

      let message = `Could not delete ${path}`

      if ('message' in error && typeof error.message === 'string') {
        message = error.message
      }

      return Promise.reject(message)
    }

    return Promise.resolve()
  })
}

export default useDeleteWorkspacePathMutation
