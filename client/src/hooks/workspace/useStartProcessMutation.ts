import useWorkspace from './useWorkspace'
import { useMutation } from 'react-query'

const stringifyCommands = (commands: string[]) =>
  commands.length !== 0 ? '["' + commands.join('","') + '"]' : '[]'

const START_PROCESS = (commands: string[]) => `
mutation StartProcess {
  startProcess(cmd: ${stringifyCommands(commands)}) {
    id
  }
}
`

type StartProcessMutation = {
  startProcess: {
    id: string
  }
}

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
