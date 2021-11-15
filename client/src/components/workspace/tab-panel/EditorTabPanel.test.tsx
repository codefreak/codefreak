import { mockFetch, render, waitForTime } from '../../../services/testing'
import EditorTabPanel from './EditorTabPanel'
import {
  NO_ANSWER_ID,
  NO_AUTH_TOKEN,
  NO_TASK_ID
} from '../../../hooks/workspace/useWorkspace'

describe('<EditorTabPanel />', () => {
  const mockFileContents = 'Hello world!'

  beforeEach(() => {
    mockFetch(mockFileContents)
  })

  it('renders a <TabPanel />', async () => {
    const baseUrl = 'https://codefreak.test'
    const authToken = NO_AUTH_TOKEN
    const answerId = NO_ANSWER_ID
    const taskId = NO_TASK_ID

    const { container } = render(
      <EditorTabPanel file="file" />,
      {},
      {
        withWorkspaceContextProvider: true,
        workspaceContext: {
          isAvailable: true,
          baseUrl,
          authToken,
          answerId,
          taskId
        }
      }
    )

    await waitForTime()

    expect(
      container.getElementsByClassName('workspace-tab-panel')
    ).toHaveLength(1)
  })

  it('shows a placeholder when still loading', () => {
    const { container } = render(<EditorTabPanel file="file" />)

    expect(container.textContent).toBe('')
    expect(
      container.getElementsByClassName('workspace-tab-panel-placeholder')
    ).toHaveLength(1)
  })
})
