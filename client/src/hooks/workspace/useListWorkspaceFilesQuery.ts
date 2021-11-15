import useWorkspace from './useWorkspace'
import { Client } from 'graphql-ws'
import useWorkspaceBaseQuery from './useWorkspaceBaseQuery'
import GraphqlWsErrorType from '../../errors/GraphqlWsErrorType'

/**
 * Returns the GraphQL query for listing the files of a given path as a string
 *
 * @param path the path to list files in
 * @returns the GraphQL query for listing the files of a given path as a string
 */
const LIST_FILES = (path: string) => `
  query ListFiles {
    listFiles(path: "${path}") {
      path
      ... on File {
        size
      }
    }
  }
`

/**
 * The type of object returned by the ListFiles query
 */
type ListFilesReturnType = {
  listFiles: FileSystemNode[]
}

/**
 * Represents a file-systen node with a path and an optional size if it is a file
 */
export type FileSystemNode = {
  /**
   * The path of the node
   */
  path: string

  /**
   * The optional size of the node if it is a file
   */
  size?: number
}

/**
 * Queries the file-system nodes in a path
 *
 * @param path the path
 * @param graphqlWebSocketClient the GraphQL client to use for the query
 * @returns a promise with the file-system nodes in the path
 */
export const listFiles = (path: string, graphqlWebSocketClient: Client) =>
  new Promise<FileSystemNode[]>((resolve, reject) => {
    let result: FileSystemNode[] = []
    graphqlWebSocketClient.subscribe<ListFilesReturnType>(
      { query: LIST_FILES(path) },
      {
        next: value => {
          if (value.data?.listFiles) {
            result = value.data.listFiles
          }
        },
        complete: () => resolve(result),
        error: error => {
          if ((error as GraphqlWsErrorType).reason !== undefined) {
            let reason = (error as GraphqlWsErrorType).reason

            if (reason === 'Invalid message') {
              // graphql-ws cannot parse the error message
              reason = `Could not list contents of directory ${path}`
            }

            return reject(reason)
          }

          return reject()
        }
      }
    )
  })

/**
 * Queries the file-system nodes in the root path of a workspace
 *
 * @returns a query for the file-system nodes in the root path of a workspace
 */
const useListWorkspaceFilesQuery = () => {
  const { baseUrl, graphqlWebSocketClient } = useWorkspace()
  return useWorkspaceBaseQuery(
    ['workspace-list-files', baseUrl],
    () => {
      if (!graphqlWebSocketClient) {
        throw new Error('No graphql websocket client found')
      }

      return listFiles('/', graphqlWebSocketClient)
    },
    { enabled: baseUrl.length > 0 && graphqlWebSocketClient !== undefined }
  )
}

export default useListWorkspaceFilesQuery
