import { useMutation, useQueryClient } from 'react-query'
import useWorkspace from './useWorkspace'
import {
  readFilePath,
  WRITE_FILE_FORM_KEY,
  writeFilePath
} from '../../services/workspace'

const useSaveWorkspaceFileMutation = () => {
  const { baseUrl } = useWorkspace()
  const queryClient = useQueryClient()
  const fullPath = writeFilePath(baseUrl)
  return useMutation(
    ['get-workspace-file', fullPath],
    ({ path, contents }: { path: string; contents: string }) => {
      if (baseUrl.length === 0) {
        return Promise.reject('No base-url for the workspace given')
      }

      const formData = new FormData()
      const file = new File([contents], path)
      formData.append(WRITE_FILE_FORM_KEY, file, path)

      return fetch(fullPath, { method: 'POST', body: formData })
    },
    {
      onSuccess: (_, { path }) => {
        return queryClient.invalidateQueries([
          'get-workspace-file',
          readFilePath(baseUrl, path)
        ])
      }
    }
  )
}

export default useSaveWorkspaceFileMutation
