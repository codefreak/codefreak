import { act } from '@testing-library/react'
import { renderHook } from '@testing-library/react-hooks'
import useWorkspace from './useWorkspace'
import { readFilePath } from '../services/workspace'

describe('useWorkspace()', () => {
  it('gets file-contents from the correct endpoint', async () => {
    const mockFileContents = 'Hello world!'
    const baseUrl = 'https://codefreak.test/'

    const mockGetFile = jest.spyOn(global, 'fetch').mockImplementation(() => {
      const response = new Response(mockFileContents)
      return Promise.resolve(response)
    })

    let fileContents = ''

    await act(async () => {
      const { result } = renderHook(() => useWorkspace(baseUrl))
      fileContents = await result.current.getFile('file.txt')
    })

    expect(mockGetFile).toHaveBeenCalled()
    expect(mockGetFile).toHaveBeenCalledWith(
      readFilePath(baseUrl, 'file.txt'),
      {
        method: 'GET'
      }
    )

    expect(fileContents).toBe(mockFileContents)
  })
})
