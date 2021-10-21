import React from 'react'
import { act } from '@testing-library/react'
import { renderHook } from '@testing-library/react-hooks'
import { uploadFilePath } from '../../services/workspace'
import useSaveWorkspaceFileMutation from './useSaveWorkspaceFileMutation'
import { mockFetch, wrap } from '../../services/testing'
import { NO_ANSWER_ID, NO_AUTH_TOKEN } from './useWorkspace'

describe('useSaveWorkspaceFileMutation()', () => {
  it('saves files to the correct endpoint', async () => {
    const mockFileContents = 'Hello world!'
    const baseUrl = 'https://codefreak.test/'
    const authToken = NO_AUTH_TOKEN
    const answerId = NO_ANSWER_ID

    const mockGetFile = mockFetch()

    const wrapper = ({ children }: React.PropsWithChildren<unknown>) =>
      wrap(<>{children}</>, {
        workspaceContext: { baseUrl, authToken, answerId },
        withWorkspaceContextProvider: true
      })

    await act(async () => {
      const { result } = renderHook(() => useSaveWorkspaceFileMutation(), {
        wrapper
      })
      result.current.mutate({ path: 'file.txt', contents: mockFileContents })
    })

    expect(mockGetFile).toHaveBeenCalled()
    expect(mockGetFile).toHaveBeenCalledWith(
      uploadFilePath(baseUrl),
      expect.objectContaining({
        method: 'POST'
      })
    )
  })
})
