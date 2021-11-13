import useWorkspace, { NO_BASE_URL } from './useWorkspace'
import { useQuery } from 'react-query'
import { fetchWithAuthentication, readFilePath } from '../../services/workspace'

/**
 * Loads the contents of a file from the workspace
 */
const useGetWorkspaceFileQuery = (path: string) => {
  const { baseUrl, authToken } = useWorkspace()
  const fullPath = readFilePath(baseUrl, path)
  return useQuery(
    ['get-workspace-file', fullPath],
    async () => {
      const response = await fetchWithAuthentication(fullPath, {
        method: 'GET',
        authToken
      })

      if (!response.ok) {
        throw new Error('File does not exist')
      }

      return response.text()
    },
    { enabled: baseUrl !== NO_BASE_URL }
  )
}

export default useGetWorkspaceFileQuery
