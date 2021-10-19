import React from 'react'
import { act } from '@testing-library/react'
import { renderHook } from '@testing-library/react-hooks'
import { writeFilePath } from '../../services/workspace'
import useSaveWorkspaceFileMutation from './useSaveWorkspaceFileMutation'
import { wrap } from '../../services/testing'

describe('useSaveWorkspaceFileMutation()', () => {
  it('saves files to the correct endpoint', async () => {
    const mockFileContents = 'Hello world!'
    const baseUrl = 'https://codefreak.test/'

    const mockGetFile = jest.spyOn(global, 'fetch').mockImplementation(() => {
      const response = new Response()
      return Promise.resolve(response)
    })

    const wrapper = ({ children }: React.PropsWithChildren<unknown>) =>
      wrap(<>{children}</>, {
        workspaceContext: { baseUrl, answerId: '' },
        withWorkspaceContext: true
      })

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
