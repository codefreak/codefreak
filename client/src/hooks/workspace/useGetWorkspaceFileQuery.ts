import useWorkspace from './useWorkspace'
import { useQuery } from 'react-query'
import { readFilePath } from '../../services/workspace'

const useGetWorkspaceFileQuery = (path: string) => {
  const { baseUrl } = useWorkspace()
  const fullPath = readFilePath(baseUrl, path)
  return useQuery(
    ['get-workspace-file', fullPath],
    async () => {
      const response = await fetch(fullPath, { method: 'GET' })

      if (!response.ok) {
        throw new Error('File does not exist')
      }

      return response.text()
    },
    { enabled: baseUrl.length > 0 }
  )
}

export default useGetWorkspaceFileQuery
