import useWorkspace from './useWorkspace'
import { useQuery } from 'react-query'
import { readFilePath } from '../../services/workspace'

const useGetWorkspaceFileQuery = (path: string) => {
  const { baseUrl } = useWorkspace()
  const fullPath = readFilePath(baseUrl, path)
  return useQuery(
    ['get-workspace-file', fullPath],
    () =>
      fetch(fullPath, { method: 'GET' }).then(value => {
        if (value.ok) {
          return value.text()
        }

        return Promise.reject('File does not exist')
      }),
    { enabled: baseUrl.length > 0 }
  )
}

export default useGetWorkspaceFileQuery
