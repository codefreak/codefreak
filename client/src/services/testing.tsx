import { render as originalRender, RenderOptions } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from 'react-query'
import { MockedProvider } from '@apollo/client/testing'
import React from 'react'

type CustomRenderOptions = {
  queryClient?: QueryClient
}

export const render = (
  ui: React.ReactElement,
  options: RenderOptions & CustomRenderOptions = {}
) => {
  const { queryClient = new QueryClient(), ...renderOptions } = options
  return originalRender(
    <MockedProvider>
      <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
    </MockedProvider>,
    renderOptions
  )
}
