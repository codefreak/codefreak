import { renderHook } from '@testing-library/react-hooks'
import useWorkspace from './useWorkspace'
import React from 'react'
import { wrap } from '../../services/testing'

describe('useWorkspace()', () => {
  it('returns the workspace-context', async () => {
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
    expect(result.current.isAvailable).toBe(true)
    expect(result.current.baseUrl).toStrictEqual(baseUrl)
    expect(result.current.authToken).toStrictEqual(authToken)
    expect(result.current.answerId).toStrictEqual(answerId)
    expect(result.current.taskId).toStrictEqual(taskId)
  })
})
