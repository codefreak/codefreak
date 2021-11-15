import { renderHook } from '@testing-library/react-hooks'
import useWorkspace from './useWorkspace'
import React from 'react'
import { mockFetch, wrap } from '../../services/testing'

describe('useWorkspace()', () => {
  it('checks whether the workspace is available and returns the workspace-context', async () => {
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

    const { result } = renderHook(() => useWorkspace(), {
      wrapper
    })

    // The second object is needed in case a authorization header is sent
    expect(fetchMock).toHaveBeenCalledWith(baseUrl, expect.objectContaining({}))
    expect(result.current.isAvailable).toBe(true)
    expect(result.current.baseUrl).toStrictEqual(baseUrl)
    expect(result.current.authToken).toStrictEqual(authToken)
    expect(result.current.answerId).toStrictEqual(answerId)
    expect(result.current.taskId).toStrictEqual(taskId)
  })
})
