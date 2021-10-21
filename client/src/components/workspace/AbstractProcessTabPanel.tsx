import useWorkspace from '../../hooks/workspace/useWorkspace'
import { useEffect, useRef, useState } from 'react'
import { ITheme, Terminal } from 'xterm'
import useResizeProcessMutation, {
  ProcessType
} from '../../hooks/workspace/useResizeProcessMutation'
import { FitAddon } from 'xterm-addon-fit'
import { debounce } from 'ts-debounce'
import { processWebSocketPath } from '../../services/workspace'
import { AttachAddon } from 'xterm-addon-attach'
import TabPanel from './TabPanel'

const XTermThemeLight: ITheme = {
  foreground: '#303030',
  background: '#ffffff',
  cursor: '#303030',
  cursorAccent: '#505050',
  selection: '#05303030',
  black: '#151515',
  red: '#ac4142',
  green: '#90a959',
  yellow: '#f4bf75',
  blue: '#6a9fb5',
  magenta: '#aa759f',
  cyan: '#75b5aa',
  white: '#d0d0d0',
  brightBlack: '#505050',
  brightRed: '#ac4142',
  brightGreen: '#90a959',
  brightYellow: '#f4bf75',
  brightBlue: '#6a9fb5',
  brightMagenta: '#aa759f',
  brightCyan: '#75b5aa',
  brightWhite: '#f5f5f5'
}

type AbstractProcessTabPanelProps = {
  processId: string
  type: ProcessType
}

const AbstractProcessTabPanel = ({
  processId,
  type
}: AbstractProcessTabPanelProps) => {
  const { baseUrl, graphqlWebSocketClient } = useWorkspace()
  const [terminal, setTerminal] = useState<Terminal>()
  const [processWebSocket, setProcessWebSocket] = useState<WebSocket>()
  const { mutate: resizeProcess } = useResizeProcessMutation(type)
  const [initialized, setInitialized] = useState(false)
  const terminalRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (terminalRef.current && !terminal) {
      const newTerminal = new Terminal({ theme: XTermThemeLight })
      newTerminal.open(terminalRef.current)
      setTerminal(newTerminal)
    }
  }, [processId, terminalRef, terminal, baseUrl])

  useEffect(() => {
    if (terminal && !processWebSocket) {
      const fitAddon = new FitAddon()
      terminal.loadAddon(fitAddon)
      fitAddon.fit()

      const resizeHandler = debounce(() => {
        fitAddon.fit()
      }, 100)
      window.addEventListener('resize', resizeHandler)

      const url = processWebSocketPath(baseUrl, processId)
      const webSocket = new WebSocket(url)

      setProcessWebSocket(webSocket)
    }
  }, [baseUrl, processId, terminal, processWebSocket])

  useEffect(() => {
    if (
      terminal &&
      processWebSocket &&
      graphqlWebSocketClient &&
      !initialized
    ) {
      const onComplete = () => {
        // Attach only after the initial resizing so its size is set-up correctly
        const attachAddon = new AttachAddon(processWebSocket)
        terminal.loadAddon(attachAddon)
      }

      // Do an initial resizing of the process so it is in sync with the rendered terminal
      resizeProcess({
        processId,
        cols: terminal.cols,
        rows: terminal.rows,
        onComplete: onComplete
      })

      terminal.onResize(
        debounce(
          ({ cols, rows }) => resizeProcess({ processId, cols, rows }),
          100
        )
      )

      setInitialized(true)
    }
  }, [
    graphqlWebSocketClient,
    initialized,
    processId,
    processWebSocket,
    resizeProcess,
    terminal
  ])

  return (
    <TabPanel withPadding>
      <div className="shell-root" ref={terminalRef} />
    </TabPanel>
  )
}

export default AbstractProcessTabPanel
