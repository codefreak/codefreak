import { renderHook } from '@testing-library/react-hooks'
import useWorkspace from './useWorkspace'
import React from 'react'
import { mockFetch, wrap } from '../../services/testing'

describe('useWorkspace()', () => {
  it('checks whether the workspace is available and returns the workspace-context', async () => {
    const fetchMock = mockFetch()

    const baseUrl = 'https://codefreak.test'
    const answerId = 'answerId'

    const wrapper = ({ children }: React.PropsWithChildren<unknown>) =>
      wrap(<>{children}</>, {
        workspaceContext: {
          baseUrl,
          answerId
        },
        withWorkspaceContext: true
      })

    const { result, waitForValueToChange } = renderHook(() => useWorkspace(), {
      wrapper
    })

    await waitForValueToChange(() => result.current.isAvailable)

    expect(fetchMock).toHaveBeenCalledWith(baseUrl)
    expect(result.current.isAvailable).toBe(true)
    expect(result.current.baseUrl).toStrictEqual(baseUrl)
    expect(result.current.answerId).toStrictEqual(answerId)
  })
})
