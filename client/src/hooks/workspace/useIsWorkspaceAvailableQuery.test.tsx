import { renderHook } from '@testing-library/react-hooks'
import { mockFetch, waitForTime, wrap } from '../../services/testing'
import useIsWorkspaceAvailableQuery from './useIsWorkspaceAvailableQuery'
import React from 'react'

describe('useIsWorkspaceAvailable()', () => {
  it('checks whether the workspace is available', async () => {
    const fetchMock = mockFetch()

    const baseUrl = 'https://codefreak.test'
    const authToken = 'authToken'
    const answerId = 'answerId'
    const taskId = 'taskId'

    const wrapper = ({ children }: React.PropsWithChildren<unknown>) =>
      wrap(<>{children}</>, {
        workspaceContext: {
          isAvailable: true,
          baseUrl,
          authToken,
          answerId,
          taskId
        },
        withWorkspaceContextProvider: true
      })

    const { result } = renderHook(
      () => useIsWorkspaceAvailableQuery(baseUrl, authToken),
      { wrapper }
    )

    await waitForTime()

    // The second object is needed in case a authorization header is sent
    expect(fetchMock).toHaveBeenCalledWith(baseUrl, expect.objectContaining({}))
    expect(result.current).toBe(true)
  })
})
