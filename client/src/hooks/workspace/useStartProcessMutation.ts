import useWorkspace from './useWorkspace'
import useWorkspaceBaseMutation from './useWorkspaceBaseMutation'
import GraphqlWsErrorType from '../../errors/GraphqlWsErrorType'

/**
 * Connects the given commands to a string containing an array of strings
 */
const stringifyCommands = (commands: string[]) =>
  commands.length !== 0 ? '["' + commands.join('","') + '"]' : '[]'

/**
 * The GraphQL mutation to start a process with the given commands
 */
const START_PROCESS = (commands: string[]) => `
mutation StartProcess {
  startProcess(cmd: ${stringifyCommands(commands)}) {
    id
  }
}
`

/**
 * Return-type of the StartProcessMutation
 */
type StartProcessMutation = {
  startProcess: {
    id: string
  }
}

/**
 * Starts a process with the given commands in the workspace.
 * When no commands are given a basic bash will be started.
 */
const useStartProcessMutation = (commands = ['bash']) => {
  const { graphqlWebSocketClient } = useWorkspace()

  return useWorkspaceBaseMutation(
    () =>
      new Promise<string>((resolve, reject) => {
        let processId: string | undefined

        if (!graphqlWebSocketClient) {
          throw new Error('GraphQL WebSocket client is not ready yet')
        }

        graphqlWebSocketClient.subscribe<StartProcessMutation>(
          { query: START_PROCESS(commands) },
          {
            next: data => (processId = data.data?.startProcess.id),
            error: error => {
              if ((error as GraphqlWsErrorType).reason !== undefined) {
                let reason = (error as GraphqlWsErrorType).reason

                if (reason === 'Invalid message') {
                  // graphql-ws cannot parse the error message
                  reason = 'Could not start process'
                }

                return reject(reason)
              }

              return reject()
            },
            complete: () => {
              if (!processId) {
                throw new Error('No process id was returned')
              }
              return resolve(processId)
            }
          }
        )
      }),
    {},
    { disableGlobalErrorHandling: true }
  )
}

export default useStartProcessMutation
