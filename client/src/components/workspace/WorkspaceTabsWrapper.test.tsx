import { render, waitForTime, wrap } from '../../services/testing'
import WorkspaceTabsWrapper from './WorkspaceTabsWrapper'
import { WorkspaceTab } from '../../services/workspace-tabs'
import { QueryClient } from 'react-query'
import React from 'react'
import { renderHook } from '@testing-library/react-hooks'
import useWorkspace from '../../hooks/workspace/useWorkspace'
import { EditorWorkspaceTab } from './EditorTabPanel'

describe('<WorkspaceTabsWrapper />', () => {
  it('renders an <EmptyTabPanel /> when no tabs are given', () => {
    const { container } = render(<WorkspaceTabsWrapper tabs={[]} />)

    expect(container.textContent).toContain('No files open')
  })

  it('renders given tabs', async () => {
    jest
      .spyOn(global, 'fetch')
      .mockImplementation(() => Promise.resolve(new Response()))

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

    const tabs: WorkspaceTab[] = [new EditorWorkspaceTab('foo.txt')]

    const { container } = render(
      <WorkspaceTabsWrapper tabs={tabs} />,
      {},
      {
        withWorkspaceContext: true,
        queryClient,
        workspaceContext: { baseUrl, answerId: '' }
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
