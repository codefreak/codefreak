import { useMutation, useQueryClient } from 'react-query'
import useWorkspace, { NO_BASE_URL } from './useWorkspace'
import {
  fetchWithAuthentication,
  readFilePath,
  UPLOAD_FILE_FORM_KEY,
  uploadFilePath
} from '../../services/workspace'

/**
 * Returns a function to upload file-contents to the workspace
 */
const useSaveWorkspaceFileMutation = () => {
  const { baseUrl, authToken } = useWorkspace()
  const queryClient = useQueryClient()
  const fullPath = uploadFilePath(baseUrl)
  return useMutation(
    ['get-workspace-file', fullPath],
    ({ path, contents }: { path: string; contents: string }) => {
      if (baseUrl === NO_BASE_URL) {
        return Promise.reject('No base-url for the workspace given')
      }

      const formData = new FormData()
      const file = new File([contents], path)
      formData.append(UPLOAD_FILE_FORM_KEY, file, path)

      return fetchWithAuthentication(fullPath, {
        method: 'POST',
        body: formData,
        authToken
      })
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
