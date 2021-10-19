import { mockFetch, render, wrap } from '../../services/testing'
import EditorTabPanel from './EditorTabPanel'
import { QueryClient } from 'react-query'
import { renderHook } from '@testing-library/react-hooks'
import useWorkspace from '../../hooks/workspace/useWorkspace'
import React from 'react'

describe('<EditorTabPanel />', () => {
  const mockFileContents = 'Hello world!'

  beforeEach(() => {
    mockFetch(mockFileContents)
  })

  it('renders a <TabPanel />', async () => {
    const queryClient = new QueryClient()
    const baseUrl = 'https://codefreak.test'
    const wrapper = ({ children }: React.PropsWithChildren<unknown>) =>
      wrap(<>{children}</>, {
        queryClient,
        workspaceContext: { baseUrl, answerId: '' },
        withWorkspaceContext: true
      })

    const { waitFor, result } = renderHook(() => useWorkspace(), { wrapper })
    await waitFor(() => result.current.isAvailable)

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
