import { mockFetch, render, waitForTime } from '../../services/testing'
import WorkspacePage from './WorkspacePage'
import {
  FileContextType,
  StartWorkspaceDocument
} from '../../services/codefreak-api'
import { noop } from '../../services/util'
import { MockedResponse } from '@apollo/client/testing'
import { NO_AUTH_TOKEN, NO_TASK_ID } from '../../hooks/workspace/useWorkspace'

const startWorkspaceMock: (
  onResult: () => void,
  baseUrl: string,
  answerId: string
) => MockedResponse = (
  onResult: () => void,
  baseUrl: string,
  answerId: string
) => ({
  request: {
    query: StartWorkspaceDocument,
    variables: {
      context: {
        id: answerId,
        type: FileContextType.Answer
      }
    }
  },
  result: () => {
    onResult()
    return {
      data: {
        startWorkspace: {
          baseUrl
        }
      }
    }
  }
})

const baseUrl = 'https://codefreak.test'
const authToken = NO_AUTH_TOKEN
const answerId = 'foo'
const taskId = NO_TASK_ID

describe('<WorkspacePage />', () => {
  beforeEach(() => {
    mockFetch()
  })

  it('renders three <WorkspaceTabsWrapper /> areas', () => {
    mockFetch()

    const workspaceContext = {
      isAvailable: true,
      baseUrl,
      authToken,
      answerId,
      taskId
    }

    const mocks = [startWorkspaceMock(noop, baseUrl, answerId)]

    const { container } = render(
      <WorkspacePage
        onBaseUrlChange={noop}
        type={FileContextType.Answer}
        createAnswerButton={null}
      />,
      {},
      {
        workspaceContext,
        graphqlMocks: mocks,
        withWorkspaceContextProvider: true
      }
    )

    expect(
      container.getElementsByClassName('workspace-tabs-wrapper')
    ).toHaveLength(3)
  })

  it('starts a workspace and the correct baseUrl is set', async () => {
    let wasStartWorkspaceCalled = false
    let baseUrlFromProvider = ''

    const workspaceContext = {
      isAvailable: false,
      baseUrl: baseUrlFromProvider,
      authToken,
      answerId,
      taskId
    }

    const mocks = [
      startWorkspaceMock(
        () => (wasStartWorkspaceCalled = true),
        baseUrl,
        answerId
      )
    ]

    render(
      <WorkspacePage
        onBaseUrlChange={newBaseUrl => (baseUrlFromProvider = newBaseUrl)}
        type={FileContextType.Answer}
        createAnswerButton={null}
      />,
      {},
      {
        workspaceContext,
        graphqlMocks: mocks,
        withWorkspaceContextProvider: true
      }
    )

    await waitForTime()

    expect(wasStartWorkspaceCalled).toBe(true)
    expect(baseUrlFromProvider).toBe(baseUrl)
  })
})
