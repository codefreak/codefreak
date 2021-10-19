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
  WorkspaceContext,
  WorkspaceContextType
} from '../hooks/workspace/useWorkspace'

type CustomRenderOptions = {
  routerHistory?: History
  queryClient?: QueryClient
  graphqlMocks?: MockedResponse[]
  workspaceContext?: WorkspaceContextType
  withRouter?: boolean
  withMockedProvider?: boolean
  withQueryClientProvider?: boolean
  withWorkspaceContext?: boolean
}

export const wrap = (
  ui: React.ReactElement,
  options: CustomRenderOptions = {}
) => {
  const {
    routerHistory = createMemoryHistory(),
    queryClient = new QueryClient(),
    graphqlMocks = [],
    workspaceContext = { baseUrl: '', answerId: '' },
    withRouter = true,
    withMockedProvider = true,
    withQueryClientProvider = true,
    withWorkspaceContext = false
  } = options

  let wrappedUi = ui

  if (withWorkspaceContext) {
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

export const render = (
  ui: React.ReactElement,
  options: RenderOptions = {},
  customOptions: CustomRenderOptions = {}
) => {
  const wrappedUi = wrap(ui, customOptions)
  return originalRender(wrappedUi, options)
}

export const waitForTime = async (ms = 0) =>
  act(async () => {
    await new Promise(resolve => setTimeout(resolve, ms))
  })
