import useWorkspace, { NO_AUTH_TOKEN, NO_BASE_URL } from './useWorkspace'
import { fetchWithAuthentication, readFilePath } from '../../services/workspace'
import useWorkspaceBaseQuery from './useWorkspaceBaseQuery'

/**
 * Loads the contents of a file from the workspace
 */
const useGetWorkspaceFileQuery = (path: string) => {
  const { baseUrl, authToken } = useWorkspace()
  const fullPath = readFilePath(baseUrl, path)
  return useWorkspaceBaseQuery(
    ['get-workspace-file', fullPath],
    async () => {
      const response = await fetchWithAuthentication(fullPath, {
        method: 'GET',
        authToken: authToken ?? NO_AUTH_TOKEN
      })

      if (!response.ok) {
        const error = await response.json()

        let message = `Could not get contents of file ${path}`

        if ('message' in error && typeof error.message === 'string') {
          message = error.message
        }

        return Promise.reject(message)
      }

      return response.text()
    },
    { enabled: baseUrl !== NO_BASE_URL && authToken !== undefined }
  )
}

export default useGetWorkspaceFileQuery
