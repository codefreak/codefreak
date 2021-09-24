import { useQuery } from 'react-query'
import useWorkspace from './useWorkspace'
import { Client } from 'graphql-ws'

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

type ListFilesType = {
  listFiles: FileSystemNode[]
}

export type FileSystemNode = {
  path: string
  size?: number
}

export const listFiles = (path: string, graphqlWebSocketClient: Client) =>
  new Promise<FileSystemNode[]>((resolve, reject) => {
    let result: FileSystemNode[] = []
    graphqlWebSocketClient.subscribe<ListFilesType>(
      { query: LIST_FILES(path) },
      {
        next: value => {
          if (value.data?.listFiles) {
            result = value.data.listFiles
          }
        },
        complete: () => resolve(result),
        error: reject
      }
    )
  })

const useListWorkspaceFilesQuery = () => {
  const { baseUrl, graphqlWebSocketClient } = useWorkspace()
  return useQuery(
    ['workspace-list-files', baseUrl],
    () => {
      return graphqlWebSocketClient
        ? listFiles('/', graphqlWebSocketClient)
        : Promise.reject('No graphql websocket client found')
    },
    { enabled: baseUrl.length > 0 && graphqlWebSocketClient !== undefined }
  )
}

export default useListWorkspaceFilesQuery
