import {
  mockFetch,
  render,
  waitUntilWorkspaceIsAvailable
} from '../../services/testing'
import EditorTabPanel from './EditorTabPanel'
import { QueryClient } from 'react-query'
import {
  NO_ANSWER_ID,
  NO_AUTH_TOKEN,
  NO_TASK_ID
} from '../../hooks/workspace/useWorkspace'

describe('<EditorTabPanel />', () => {
  const mockFileContents = 'Hello world!'

  beforeEach(() => {
    mockFetch(mockFileContents)
  })

  it('renders a <TabPanel />', async () => {
    const queryClient = new QueryClient()
    const baseUrl = 'https://codefreak.test'
    const authToken = NO_AUTH_TOKEN
    const answerId = NO_ANSWER_ID
    const taskId = NO_TASK_ID

    await waitUntilWorkspaceIsAvailable({
      queryClient,
      baseUrl,
      authToken,
      answerId,
      taskId
    })

    const { container } = render(
      <EditorTabPanel file="file" />,
      {},
      {
        queryClient
      }
    )

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
