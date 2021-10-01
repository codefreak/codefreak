import { QueryClient, QueryClientProvider } from 'react-query'
import React from 'react'
import { WorkspaceContext } from './useWorkspace'
import { act } from '@testing-library/react'
import { renderHook } from '@testing-library/react-hooks'
import { writeFilePath } from '../../services/workspace'
import useSaveWorkspaceFileMutation from './useSaveWorkspaceFileMutation'

describe('useSaveWorkspaceFileMutation()', () => {
  it('saves files to the correct endpoint', async () => {
    const mockFileContents = 'Hello world!'
    const baseUrl = 'https://codefreak.test/'

    const mockGetFile = jest.spyOn(global, 'fetch').mockImplementation(() => {
      const response = new Response()
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

    await act(async () => {
      const { result } = renderHook(() => useSaveWorkspaceFileMutation(), {
        wrapper
      })
      result.current.mutate({ path: 'file.txt', contents: mockFileContents })
    })

    expect(mockGetFile).toHaveBeenCalled()
    expect(mockGetFile).toHaveBeenCalledWith(
      writeFilePath(baseUrl),
      expect.objectContaining({
        method: 'POST'
      })
    )
  })
})
