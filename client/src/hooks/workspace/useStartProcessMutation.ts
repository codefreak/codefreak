import useWorkspace from './useWorkspace'
import { useMutation } from 'react-query'

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
  const { graphqlWebSocketClient, baseUrl } = useWorkspace()

  return useMutation(
    ['workspace-start-process', baseUrl, stringifyCommands(commands)],
    () =>
      new Promise<string>((resolve, reject) => {
        let processId: string | undefined

        if (!graphqlWebSocketClient) {
          reject('GraphQL WebSocket client is not ready yet')
          return
        }

        graphqlWebSocketClient.subscribe<StartProcessMutation>(
          { query: START_PROCESS(commands) },
          {
            next: data => (processId = data.data?.startProcess.id),
            error: reject,
            complete: () => {
              if (!processId) {
                return reject('No process id was returned')
              }
              return resolve(processId)
            }
          }
        )
      })
  )
}

export default useStartProcessMutation
