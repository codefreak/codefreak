import { createContext, useContext } from 'react'
import { useQuery } from 'react-query'

export type WorkspaceContextType = {
  baseUrl: string
  answerId: string
}

const initialWorkspaceContext: WorkspaceContextType = {
  baseUrl: '',
  answerId: ''
}

export const WorkspaceContext = createContext<WorkspaceContextType>(
  initialWorkspaceContext
)

const useWorkspace = () => {
  const context = useContext(WorkspaceContext)
  const { data } = useQuery(
    'isWorkspaceAvailable',
    () => fetch(context.baseUrl).then(() => Promise.resolve(true)),
    {
      enabled: context.baseUrl.length > 0,
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
