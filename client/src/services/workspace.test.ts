import {
  extractRelativeFilePath,
  fetchWithAuthentication,
  readFilePath,
  uploadFilePath
} from './workspace'
import { mockFetch } from './testing'

test('extractRelativeFilePath', () => {
  new Map([
    ['https://codefreak.test/files/foo.txt', 'foo.txt'],
    ['https://codefreak.test/files/bar/foo.txt', 'bar/foo.txt'],
    ['https://codefreak.test/files/', ''],
    ['https://codefreak.test/foo.txt', null],
    ['/files/foo.txt', null],
    ['foo.txt', null],
    ['', null]
  ]).forEach((expected, input) => {
    if (expected === null) {
      expect(() => extractRelativeFilePath(input)).toThrow()
    } else {
      expect(extractRelativeFilePath(input)).toBe(expected)
    }
  })
})

test('uploadFilePath', () => {
  new Map([
    ['https://codefreak.test/', 'https://codefreak.test/upload'],
    ['https://codefreak.test', 'https://codefreak.test/upload']
  ]).forEach((expected, input) => {
    expect(uploadFilePath(input)).toBe(expected)
  })
})

test('readFilePath', () => {
  new Map([
    ['foo.txt', 'https://codefreak.test/files/foo.txt'],
    ['bar/foo.txt', 'https://codefreak.test/files/bar/foo.txt'],
    ['/foo.txt', 'https://codefreak.test/files/foo.txt'],
    ['/bar/foo.txt', 'https://codefreak.test/files/bar/foo.txt'],
    ['', 'https://codefreak.test/files/'],
    ['/', 'https://codefreak.test/files/']
  ]).forEach((expected, input) => {
    expect(readFilePath('https://codefreak.test', input)).toBe(expected)
  })
})

test('fetchWithAuthentication', async () => {
  const authToken = 'auth'

  let fetchAuthToken = ''

  mockFetch(null, undefined, (_, init) => {
    if (
      init !== undefined &&
      init.headers !== undefined &&
      'Authorization' in init.headers
    ) {
      fetchAuthToken = init.headers.Authorization
    }
  })

  await fetchWithAuthentication('https://codefreak.test/', { authToken })

  expect(fetchAuthToken).toContain(authToken)
})
