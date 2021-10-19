import { renderHook } from '@testing-library/react-hooks'
import { readFilePath } from '../../services/workspace'
import React from 'react'
import { QueryClient, setLogger } from 'react-query'
import useGetWorkspaceFileQuery from './useGetWorkspaceFileQuery'
import { mockFetch, waitForTime, wrap } from '../../services/testing'

describe('useGetWorkspaceFileQuery()', () => {
  const mockFileContents = 'Hello world!'
  let mockGetFile: jest.SpyInstance | null = null

  beforeEach(() => {
    mockGetFile = mockFetch(mockFileContents)
  })

  it('gets file-contents from the correct endpoint', async () => {
    const baseUrl = 'https://codefreak.test/'

    const wrapper = ({ children }: React.PropsWithChildren<unknown>) =>
      wrap(<>{children}</>, {
        workspaceContext: { baseUrl, answerId: '' },
        withWorkspaceContext: true
      })

    const { result, waitFor } = renderHook(
      () => useGetWorkspaceFileQuery('file.txt'),
      { wrapper }
    )
    await waitFor(() => result.current.data !== undefined)
    const fileContents = result.current.data

    expect(mockGetFile).toHaveBeenCalled()
    expect(mockGetFile).toHaveBeenCalledWith(
      readFilePath(baseUrl, 'file.txt'),
      expect.objectContaining({
        method: 'GET'
      })
    )
    expect(fileContents).toBe(mockFileContents)
  })

  it('does nothing when no base-url is set', async () => {
    const baseUrl = ''

    const wrapper = ({ children }: React.PropsWithChildren<unknown>) =>
      wrap(<>{children}</>, {
        workspaceContext: { baseUrl, answerId: '' },
        withWorkspaceContext: true
      })

    const { result } = renderHook(() => useGetWorkspaceFileQuery('file.txt'), {
      wrapper
    })
    await waitForTime()
    const fileContents = result.current.data

    expect(mockGetFile).not.toHaveBeenCalled()
    expect(fileContents).toBe(undefined)
  })

  it('has an error if the file does not exist', async () => {
    const baseUrl = 'https://codefreak.test'

    mockGetFile = mockFetch(null, { status: 404 })

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } }
    })
    const wrapper = ({ children }: React.PropsWithChildren<unknown>) =>
      wrap(<>{children}</>, {
        workspaceContext: { baseUrl, answerId: '' },
        queryClient,
        withWorkspaceContext: true
      })

    setLogger({
      // eslint-disable-next-line no-console
      log: console.log,
      warn: console.warn,
      error: () => {
        // Don't log network errors in tests
      }
    })

    const { result, waitFor } = renderHook(
      () => useGetWorkspaceFileQuery('file.txt'),
      {
        wrapper
      }
    )
    await waitFor(() => !result.current.isLoading)

    expect(mockGetFile).toHaveBeenCalled()
    expect(result.current.isError).toBe(true)
  })
})
