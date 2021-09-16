import { createContext, useContext } from 'react'
import { useQuery } from 'react-query'
import { Client } from 'graphql-ws'

type WorkspaceContextType = {
  baseUrl: string
  graphqlWebSocketClient?: Client
}

const initialWorkspaceContext: WorkspaceContextType = { baseUrl: '' }

export const WorkspaceContext = createContext<WorkspaceContextType>(
  initialWorkspaceContext
)

const useWorkspace = () => {
  const { baseUrl, graphqlWebSocketClient } = useContext(WorkspaceContext)
  const { data } = useQuery(
    'isWorkspaceAvailable',
    () => fetch(baseUrl).then(() => Promise.resolve(true)),
    {
      enabled: baseUrl.length > 0,
      retry: true
    }
  )

  const isAvailable = data !== undefined && data

  return {
    isAvailable,
    baseUrl,
    graphqlWebSocketClient
  }
}

export default useWorkspace
