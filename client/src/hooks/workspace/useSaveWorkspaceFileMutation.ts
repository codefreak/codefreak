import { useQueryClient } from 'react-query'
import useWorkspace, { NO_BASE_URL } from './useWorkspace'
import {
  fetchWithAuthentication,
  readFilePath,
  UPLOAD_FILE_FORM_KEY,
  uploadFilePath
} from '../../services/workspace'
import useWorkspaceBaseMutation from './useWorkspaceBaseMutation'

/**
 * Returns a function to upload file-contents to the workspace
 */
const useSaveWorkspaceFileMutation = () => {
  const { baseUrl, authToken } = useWorkspace()
  const queryClient = useQueryClient()
  const fullPath = uploadFilePath(baseUrl)
  return useWorkspaceBaseMutation(
    async ({ path, contents }: { path: string; contents: string }) => {
      if (baseUrl === NO_BASE_URL) {
        throw new Error('No base-url for the workspace given')
      }

      const formData = new FormData()
      const file = new File([contents], path)
      formData.append(UPLOAD_FILE_FORM_KEY, file, path)

      const response = await fetchWithAuthentication(fullPath, {
        method: 'POST',
        body: formData,
        authToken
      })

      if (!response.ok) {
        const error = await response.json()

        let message = `Could not save ${path}. Beware! Further changes might not be saved as well.`

        if ('message' in error && typeof error.message === 'string') {
          message = error.message
        }

        return Promise.reject(message)
      }

      return Promise.resolve()
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
