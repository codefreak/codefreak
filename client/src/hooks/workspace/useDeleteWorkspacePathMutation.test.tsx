import { renderHook } from '@testing-library/react-hooks'
import { setLogger } from 'react-query'
import { mockFetch, waitForTime, wrap } from '../../services/testing'
import { deletePath } from '../../services/workspace'
import useDeleteWorkspacePathMutation from './useDeleteWorkspacePathMutation'
import { NO_ANSWER_ID, NO_AUTH_TOKEN, NO_TASK_ID } from './useWorkspace'

describe('useCreateWorkspacePathMutation()', () => {
  it('deletes the file at correct endpoint', async () => {
    const baseUrl = 'https://codefreak.test'
    const authToken = NO_AUTH_TOKEN
    const answerId = NO_ANSWER_ID
    const taskId = NO_TASK_ID

    const mockCreateFile = mockFetch()

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

    const { result } = renderHook(() => useDeleteWorkspacePathMutation(), {
      wrapper
    })
    result.current.mutate({ path: 'foo.txt' })

    await waitForTime()

    expect(mockCreateFile).toHaveBeenCalled()
    expect(mockCreateFile).toHaveBeenCalledWith(
      deletePath(baseUrl, 'foo.txt'),
      expect.objectContaining({
        method: 'DELETE'
      })
    )
  })

  it('errors when no base-url is set', async () => {
    const baseUrl = ''
    const authToken = NO_AUTH_TOKEN
    const answerId = NO_ANSWER_ID
    const taskId = NO_TASK_ID

    const mockCreateFile = mockFetch()

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

    setLogger({
      // eslint-disable-next-line no-console
      log: console.log,
      warn: console.warn,
      error: () => {
        // Don't log network errors in tests
      }
    })

    const { result } = renderHook(() => useDeleteWorkspacePathMutation(), {
      wrapper
    })
    result.current.mutate({ path: 'foo.txt' })

    await waitForTime()

    expect(mockCreateFile).not.toHaveBeenCalled()
    expect(result.current.isError).toBe(true)
    expect(result.current.error).toContain('No base-url')
  })

  it('errors when the api errors', async () => {
    const baseUrl = 'https://codefreak.test'
    const authToken = NO_AUTH_TOKEN
    const answerId = NO_ANSWER_ID
    const taskId = NO_TASK_ID

    const mockCreateFile = mockFetch('File does not exist', { status: 404 })

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

    setLogger({
      // eslint-disable-next-line no-console
      log: console.log,
      warn: console.warn,
      error: () => {
        // Don't log network errors in tests
      }
    })

    const { result } = renderHook(() => useDeleteWorkspacePathMutation(), {
      wrapper
    })
    result.current.mutate({ path: 'foo.txt' })

    await waitForTime()

    expect(mockCreateFile).toHaveBeenCalled()
    expect(result.current.isError).toBe(true)
    expect(result.current.error).toContain('File does not exist')
  })
})
