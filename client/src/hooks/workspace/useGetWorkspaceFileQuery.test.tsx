import { act } from '@testing-library/react'
import { renderHook } from '@testing-library/react-hooks'
import { WorkspaceContext } from './useWorkspace'
import { readFilePath } from '../../services/workspace'
import React from 'react'
import { QueryClient, QueryClientProvider } from 'react-query'
import useGetWorkspaceFileQuery from './useGetWorkspaceFileQuery'

describe('useGetWorkspaceFileQuery()', () => {
  it('gets file-contents from the correct endpoint', async () => {
    const mockFileContents = 'Hello world!'
    const baseUrl = 'https://codefreak.test/'

    const mockGetFile = jest.spyOn(global, 'fetch').mockImplementation(() => {
      const response = new Response(mockFileContents)
      return Promise.resolve(response)
    })

    const queryClient = new QueryClient()
    const wrapper = <P extends unknown>({
      children
    }: React.PropsWithChildren<P>) => (
      <QueryClientProvider client={queryClient}>
        <WorkspaceContext.Provider
          value={{ baseUrl, answerId: '', taskId: '' }}
        >
          {children}
        </WorkspaceContext.Provider>
      </QueryClientProvider>
    )

    let fileContents: string | undefined

    await act(async () => {
      const { result, waitFor } = renderHook(
        () => useGetWorkspaceFileQuery('file.txt'),
        { wrapper }
      )
      await waitFor(() => result.current.data !== undefined)
      fileContents = result.current.data
    })

    expect(mockGetFile).toHaveBeenCalled()
    expect(mockGetFile).toHaveBeenCalledWith(
      readFilePath(baseUrl, 'file.txt'),
      expect.objectContaining({
        method: 'GET'
      })
    )

    expect(fileContents).toBe(mockFileContents)
  })
})
