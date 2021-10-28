import {
  createDirectoryPath,
  createFilePath,
  deletePath,
  extractRelativeFilePath,
  fetchWithAuthentication,
  graphqlWebSocketPath,
  httpToWs,
  processWebSocketPath,
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

test('createFilePath', () => {
  new Map([
    ['foo.txt', 'https://codefreak.test/files/foo.txt'],
    ['bar/foo.txt', 'https://codefreak.test/files/bar/foo.txt'],
    ['/foo.txt', 'https://codefreak.test/files/foo.txt'],
    ['/bar/foo.txt', 'https://codefreak.test/files/bar/foo.txt'],
    ['', 'https://codefreak.test/files'],
    ['/', 'https://codefreak.test/files']
  ]).forEach((expected, input) => {
    expect(createFilePath('https://codefreak.test', input)).toBe(expected)
  })
})

test('createDirectoryPath', () => {
  new Map([
    ['foo', 'https://codefreak.test/files/foo/'],
    ['bar/foo', 'https://codefreak.test/files/bar/foo/'],
    ['/foo', 'https://codefreak.test/files/foo/'],
    ['/bar/foo', 'https://codefreak.test/files/bar/foo/'],
    ['', 'https://codefreak.test/files/'],
    ['/', 'https://codefreak.test/files/']
  ]).forEach((expected, input) => {
    expect(createDirectoryPath('https://codefreak.test', input)).toBe(expected)
  })
})

test('deletePath', () => {
  new Map([
    ['foo.txt', 'https://codefreak.test/files/foo.txt'],
    ['bar/foo.txt', 'https://codefreak.test/files/bar/foo.txt'],
    ['/foo.txt', 'https://codefreak.test/files/foo.txt'],
    ['/bar/foo.txt', 'https://codefreak.test/files/bar/foo.txt'],
    ['', 'https://codefreak.test/files'],
    ['/', 'https://codefreak.test/files']
  ]).forEach((expected, input) => {
    expect(deletePath('https://codefreak.test', input)).toBe(expected)
  })
})

test('httpToWs', () => {
  new Map([
    ['http://codefreak.test', 'ws://codefreak.test'],
    ['https://codefreak.test', 'wss://codefreak.test'],
    ['', null],
    ['file:///home/codefreak/foo.txt', null]
  ]).forEach((expected, input) => {
    if (expected === null) {
      expect(() => httpToWs(input)).toThrow()
    } else {
      expect(httpToWs(input)).toBe(expected)
    }
  })
})

test('graphqlWebSocketPath', () => {
  new Map([
    ['http://codefreak.test', 'ws://codefreak.test/graphql'],
    ['https://codefreak.test', 'wss://codefreak.test/graphql'],
    ['', null],
    ['foo', null]
  ]).forEach((expected, input) => {
    if (expected === null) {
      expect(() => graphqlWebSocketPath(input)).toThrow()
    } else {
      expect(graphqlWebSocketPath(input)).toBe(expected)
    }
  })
})

test('processWebSocketPath', () => {
  new Map([
    [
      '00000000-0000-0000-0000-000000000000',
      'wss://codefreak.test/process/00000000-0000-0000-0000-000000000000'
    ],
    ['', null]
  ]).forEach((expected, input) => {
    if (expected === null) {
      expect(() =>
        processWebSocketPath('https://codefreak.test', input)
      ).toThrow()
    } else {
      expect(processWebSocketPath('https://codefreak.test', input)).toBe(
        expected
      )
    }
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
