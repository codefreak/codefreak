import useWorkspace from './useWorkspace'
import { useMutation } from 'react-query'
import { noop } from '../../services/util'

export enum ProcessType {
  SHELL
}

const RESIZE_PROCESS = (processId: string, cols: number, rows: number) => `
mutation ResizeProcess {
  resizeProcess(cols: ${cols}, id: "${processId}", rows: ${rows})
}
`

const useResizeProcessMutation = (processType: ProcessType) => {
  const { graphqlWebSocketClient, baseUrl } = useWorkspace()

  return useMutation(
    ['workspace-resize-process', baseUrl, processType],
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
            error: reject,
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
