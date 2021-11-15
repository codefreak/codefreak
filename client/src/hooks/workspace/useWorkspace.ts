import { createContext, useContext } from 'react'
import { Client } from 'graphql-ws'

/**
 * Indicates that no base-url is set
 */
export const NO_BASE_URL = ''

/**
 * Indicates that no auth-token is set
 */
export const NO_AUTH_TOKEN = ''

/**
 * Indicates that no answer-id is set
 */
export const NO_ANSWER_ID = ''

/**
 * Indicates that no task-id is set
 */
export const NO_TASK_ID = ''

/**
 * Provides the base-url and auth-token of the current workspace, the id of the run-process and the current answer-id and task-id.
 * Also a Client for the GraphQL over WebSockets API and the availability status of the workspace is provided.
 */
export type WorkspaceContextType = {
  /**
   * Whether the workspace specified by the base-url is available
   */
  isAvailable: boolean

  /**
   * The base-url of the current workspace
   */
  baseUrl: string

  /**
   * The auth-token of the current workspace
   */
  authToken: string

  /**
   * The current answer-id
   */
  answerId: string

  /**
   * The current task-id
   */
  taskId: string

  /**
   * The Client to use for the GraphQL over WebSockets API
   */
  graphqlWebSocketClient?: Client

  /**
   * The id of the current run-process
   */
  runProcessId?: string
}

/**
 * An initial context with no values set
 */
const initialWorkspaceContext: WorkspaceContextType = {
  isAvailable: false,
  baseUrl: NO_BASE_URL,
  authToken: NO_AUTH_TOKEN,
  answerId: NO_ANSWER_ID,
  taskId: NO_TASK_ID
}

/**
 * A context providing useful globals for the current workspace
 */
export const WorkspaceContext = createContext<WorkspaceContextType>(
  initialWorkspaceContext
)

/**
 * Returns the current workspace context and checks whether the workspace is available
 */
const useWorkspace = () => {
  return useContext(WorkspaceContext)
}

export default useWorkspace
