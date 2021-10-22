import useWorkspace, { NO_BASE_URL } from './useWorkspace'
import { useMutation } from 'react-query'
import { deletePath, fetchWithAuthentication } from '../../services/workspace'

const useDeleteWorkspacePathMutation = () => {
  const { baseUrl, authToken } = useWorkspace()

  return useMutation(({ path }: { path: string }) => {
    if (baseUrl === NO_BASE_URL) {
      return Promise.reject('No base-url for the workspace given')
    }

    const fullPath = deletePath(baseUrl, path)

    return fetchWithAuthentication(fullPath, { method: 'DELETE', authToken })
  })
}

export default useDeleteWorkspacePathMutation
