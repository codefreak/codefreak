import { createContext, useContext } from 'react'
import { useQuery } from 'react-query'
import { Client } from 'graphql-ws'

type WorkspaceContextType = {
  baseUrl: string
  graphqlWebSocketClient?: Client
  taskId: string
  answerId: string
}

const initialWorkspaceContext: WorkspaceContextType = {
  baseUrl: '',
  answerId: '',
  taskId: ''
}

export const WorkspaceContext = createContext<WorkspaceContextType>(
  initialWorkspaceContext
)

const useWorkspace = () => {
  const { baseUrl, graphqlWebSocketClient, answerId, taskId } =
    useContext(WorkspaceContext)
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
    graphqlWebSocketClient,
    answerId,
    taskId
  }
}

export default useWorkspace
