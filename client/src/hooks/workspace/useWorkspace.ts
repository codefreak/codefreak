import { createContext, useContext } from 'react'
import { useQuery } from 'react-query'
import { fetchWithAuthentication } from '../../services/workspace'
import { Client } from 'graphql-ws'

export const NO_BASE_URL = ''
export const NO_AUTH_TOKEN = ''
export const NO_ANSWER_ID = ''
export const NO_TASK_ID = ''

export type WorkspaceContextType = {
  baseUrl: string
  authToken: string
  answerId: string
  taskId: string
  graphqlWebSocketClient?: Client
  runProcessId?: string
}

const initialWorkspaceContext: WorkspaceContextType = {
  baseUrl: NO_BASE_URL,
  authToken: NO_AUTH_TOKEN,
  answerId: NO_ANSWER_ID,
  taskId: NO_TASK_ID
}

export const WorkspaceContext = createContext<WorkspaceContextType>(
  initialWorkspaceContext
)

const useWorkspace = () => {
  const context = useContext(WorkspaceContext)
  const { data } = useQuery(
    'isWorkspaceAvailable',
    () =>
      fetchWithAuthentication(context.baseUrl, {
        authToken: context.authToken
      }).then(() => Promise.resolve(true)),
    {
      enabled: context.baseUrl !== NO_BASE_URL,
      // Retry indefinitely because the workspace might take some time to start
      retry: true
    }
  )

  const isAvailable = data !== undefined && data

  return {
    isAvailable,
    ...context
  }
}

export default useWorkspace
