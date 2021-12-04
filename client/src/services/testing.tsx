import {
  act,
  render as originalRender,
  RenderOptions
} from '@testing-library/react'
import { QueryClient, QueryClientProvider } from 'react-query'
import { MockedProvider, MockedResponse } from '@apollo/client/testing'
import React from 'react'
import { createMemoryHistory, History } from 'history'
import { Router } from 'react-router-dom'
import {
  NO_ANSWER_ID,
  NO_AUTH_TOKEN,
  NO_BASE_URL,
  NO_TASK_ID,
  WorkspaceContext,
  WorkspaceContextType
} from '../hooks/workspace/useWorkspace'
import { noop } from './util'
import { RequestInitWithAuthentication } from './workspace'

type CustomRenderOptions = {
  routerHistory?: History
  queryClient?: QueryClient
  graphqlMocks?: MockedResponse[]
  workspaceContext?: WorkspaceContextType
  withRouter?: boolean
  withMockedProvider?: boolean
  withQueryClientProvider?: boolean
  withWorkspaceContextProvider?: boolean
}

/**
 * Wraps an element with provided custom options (i.e. providers for testing)
 *
 * The router, mocked-provider and query-client-provider are enabled by default, other features must be enabled explicitly
 *
 * @param ui the element to wrap
 * @param options the custom options to wrap with
 */
export const wrap = (
  ui: React.ReactElement,
  options: CustomRenderOptions = {}
) => {
  const {
    routerHistory = createMemoryHistory(),
    queryClient = new QueryClient(),
    graphqlMocks = [],
    workspaceContext = {
      isAvailable: false,
      baseUrl: NO_BASE_URL,
      authToken: NO_AUTH_TOKEN,
      answerId: NO_ANSWER_ID,
      taskId: NO_TASK_ID
    },
    withRouter = true,
    withMockedProvider = true,
    withQueryClientProvider = true,
    withWorkspaceContextProvider = false
  } = options

  let wrappedUi = ui

  if (withWorkspaceContextProvider) {
    wrappedUi = (
      <WorkspaceContext.Provider value={workspaceContext}>
        {wrappedUi}
      </WorkspaceContext.Provider>
    )
  }

  if (withQueryClientProvider) {
    wrappedUi = (
      <QueryClientProvider client={queryClient}>
        {wrappedUi}
      </QueryClientProvider>
    )
  }

  if (withMockedProvider) {
    wrappedUi = (
      <MockedProvider mocks={graphqlMocks} addTypename={false}>
        {wrappedUi}
      </MockedProvider>
    )
  }

  if (withRouter) {
    wrappedUi = <Router history={routerHistory}>{wrappedUi}</Router>
  }

  return wrappedUi
}

/**
 * Wraps the test-render-method with custom options
 *
 * See `wrap` for more details
 *
 * @param ui the element to render (and wrap)
 * @param options see `render`
 * @param customOptions see `wrap`
 */
export const render = (
  ui: React.ReactElement,
  options: RenderOptions = {},
  customOptions: CustomRenderOptions = {}
) => {
  const wrappedUi = wrap(ui, customOptions)
  return originalRender(wrappedUi, options)
}

/**
 * Waits for a given time
 *
 * @param ms the time to wait
 */
export const waitForTime = async (ms = 0) =>
  act(async () => {
    await new Promise(resolve => setTimeout(resolve, ms))
  })

/**
 * Mocks the global `fetch` method and provides a custom response
 *
 * @param responseBody see `Response`
 * @param responseInit see `Response`
 * @param onResponse an optional callback before the response is returned, i.e. to ensure it was called with specific arguments
 */
export const mockFetch = (
  responseBody: BodyInit | null = null,
  responseInit?: ResponseInit,
  onResponse: (
    input: RequestInfo,
    init?: RequestInitWithAuthentication | RequestInit
  ) => void = noop
) =>
  jest
    .spyOn(global, 'fetch')
    .mockImplementation(
      (
        input: RequestInfo,
        init?: RequestInitWithAuthentication | RequestInit
      ) => {
        onResponse(input, init)
        const response = new Response(responseBody, responseInit)
        return Promise.resolve(response)
      }
    )
