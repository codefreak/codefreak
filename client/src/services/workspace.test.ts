import {
  extractRelativeFilePath,
  readFilePath,
  withTrailingSlash,
  writeFilePath
} from './workspace'

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

test('writeFilePath', () => {
  new Map([
    ['https://codefreak.test/', 'https://codefreak.test/files'],
    ['https://codefreak.test', 'https://codefreak.test/files']
  ]).forEach((expected, input) => {
    expect(writeFilePath(input)).toBe(expected)
  })
})

test('withTrailingSlash', () => {
  new Map([
    ['https://codefreak.test/', 'https://codefreak.test/'],
    ['https://codefreak.test', 'https://codefreak.test/'],
    ['', '/'],
    ['/', '/']
  ]).forEach((expected, input) => {
    expect(withTrailingSlash(input)).toBe(expected)
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
