import {
  mockFetch,
  render,
  waitForTime,
  waitUntilWorkspaceIsAvailable
} from '../../services/testing'
import WorkspaceTabsWrapper from './WorkspaceTabsWrapper'
import { WorkspaceTab } from '../../services/workspace-tabs'
import { QueryClient } from 'react-query'
import {
  NO_ANSWER_ID,
  NO_AUTH_TOKEN,
  NO_TASK_ID
} from '../../hooks/workspace/useWorkspace'
import { EditorWorkspaceTab } from './tab-panel/EditorTabPanel'

describe('<WorkspaceTabsWrapper />', () => {
  beforeEach(() => {
    mockFetch()
  })

  it('renders an <EmptyTabPanel /> when no tabs are given', () => {
    const { container } = render(<WorkspaceTabsWrapper tabs={[]} />)

    expect(container.textContent).toContain('No files open')
  })

  it('renders given tabs', async () => {
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

    const tabs: WorkspaceTab[] = [new EditorWorkspaceTab('foo.txt')]

    const { container } = render(
      <WorkspaceTabsWrapper tabs={tabs} />,
      {},
      {
        withWorkspaceContextProvider: true,
        queryClient,
        workspaceContext: { baseUrl, authToken, answerId, taskId }
      }
    )

    // Let the fetch for the file-content proceed
    await waitForTime()

    expect(
      container.getElementsByClassName('workspace-tab-panel')
    ).toHaveLength(1)
    expect(container.textContent).toContain('foo.txt')
  })
})
