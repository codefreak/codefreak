import useWorkspace from './useWorkspace'
import { noop } from '../../services/util'
import useWorkspaceBaseMutation from './useWorkspaceBaseMutation'
import GraphqlWsErrorType from '../../errors/GraphqlWsErrorType'

/**
 * The GraphQL query to resize a process with the given id, columns and rows
 */
const RESIZE_PROCESS = (processId: string, cols: number, rows: number) => `
mutation ResizeProcess {
  resizeProcess(cols: ${cols}, id: "${processId}", rows: ${rows})
}
`

/**
 * Returns a function to resize a process in the workspace
 */
const useResizeProcessMutation = () => {
  const { graphqlWebSocketClient } = useWorkspace()

  return useWorkspaceBaseMutation(
    ({
      processId,
      cols,
      rows,
      onComplete = noop
    }: {
      processId: string
      cols: number
      rows: number
      onComplete?: () => void
    }) =>
      new Promise((resolve, reject) => {
        graphqlWebSocketClient?.subscribe<{
          resizeProcess: boolean
        }>(
          {
            query: RESIZE_PROCESS(processId, cols, rows)
          },
          {
            next: noop,
            error: error => {
              if ((error as GraphqlWsErrorType).reason !== undefined) {
                let reason = (error as GraphqlWsErrorType).reason

                if (reason === 'Invalid message') {
                  // graphql-ws cannot parse the error message
                  reason = 'Could not resize process'
                }

                return reject(reason)
              }

              return reject()
            },
            complete: () => {
              onComplete()
              resolve(null)
            }
          }
        )
      })
  )
}

export default useResizeProcessMutation
