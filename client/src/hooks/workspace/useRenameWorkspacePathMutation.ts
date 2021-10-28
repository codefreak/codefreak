import { useMutation } from 'react-query'
import useWorkspace, { NO_BASE_URL } from './useWorkspace'

type RenameWorkspacePathProps = {
  /**
   * The original path to rename
   */
  sourcePath: string

  /**
   * The new name of the path
   */
  targetPath: string
}

/**
 * Returns a mutation to rename a path in a workspace.
 */
const useRenameWorkspacePathMutation = () => {
  const { baseUrl } = useWorkspace()

  return useMutation<void, void, RenameWorkspacePathProps>(
    ({ sourcePath, targetPath }) => {
      if (baseUrl === NO_BASE_URL) {
        return Promise.reject('No base-url for the workspace given')
      }

      // TODO replace when api for this operation is set
      throw new Error(
        `Cannot rename ${sourcePath} to ${targetPath} because renaming is not supported right now`
      )
    }
  )
}

export default useRenameWorkspacePathMutation
