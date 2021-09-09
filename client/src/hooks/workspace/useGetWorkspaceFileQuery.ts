import useWorkspace from './useWorkspace'
import { useQuery } from 'react-query'
import { readFilePath } from '../../services/workspace'
import useSaveWorkspaceFileMutation from './useSaveWorkspaceFileMutation'

const useGetWorkspaceFileQuery = (path: string) => {
  const { baseUrl } = useWorkspace()
  const { mutate } = useSaveWorkspaceFileMutation()
  const fullPath = readFilePath(baseUrl, path)
  return useQuery(
    ['get-workspace-file', fullPath],
    () =>
      fetch(fullPath, { method: 'GET' }).then(value => {
        if (value.ok) {
          return value.text()
        }

        mutate({ path, contents: '' })
        return Promise.resolve('')
      }),
    { enabled: baseUrl.length > 0 }
  )
}

export default useGetWorkspaceFileQuery
