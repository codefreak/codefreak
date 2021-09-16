import useWorkspace from './useWorkspace'
import { useMutation } from 'react-query'

const START_PROCESS = `
  mutation StartProcess {
    startProcess(cmd: ["bash"]) {
      id
    }
  }
`

type StartProcessMutation = {
  startProcess: {
    id: string
  }
}

const useStartProcessMutation = () => {
  const { graphqlWebSocketClient } = useWorkspace()

  return useMutation(
    ['workspace-start-process'],
    () =>
      new Promise<string>((resolve, reject) => {
        let processId: string | undefined
        graphqlWebSocketClient?.subscribe<StartProcessMutation>(
          { query: START_PROCESS },
          {
            next: data => (processId = data.data?.startProcess.id),
            error: reject,
            complete: () => resolve(processId ?? '')
          }
        )
      })
  )
}

export default useStartProcessMutation
