import { createContext, useContext } from 'react'
import { useQuery } from 'react-query'

type WorkspaceContextType = {
  baseUrl: string
}

const initialWorkspaceContext: WorkspaceContextType = { baseUrl: '' }

export const WorkspaceContext = createContext<WorkspaceContextType>(
  initialWorkspaceContext
)

const useWorkspace = () => {
  const { baseUrl } = useContext(WorkspaceContext)
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
    baseUrl
  }
}

export default useWorkspace
