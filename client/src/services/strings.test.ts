import {
  trimLeadingSlashes,
  trimTrailingSlashes,
  withLeadingSlash,
  withTrailingSlash
} from './strings'

test('trimTrailingSlashes', () => {
  new Map([
    ['https://codefreak.test//', 'https://codefreak.test'],
    ['https://codefreak.test/', 'https://codefreak.test'],
    ['https://codefreak.test', 'https://codefreak.test'],
    ['', ''],
    ['/', ''],
    ['//', '']
  ]).forEach((expected, input) => {
    expect(trimTrailingSlashes(input)).toBe(expected)
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

test('trimLeadingSlashes', () => {
  new Map([
    ['//foo.txt', 'foo.txt'],
    ['/foo.txt', 'foo.txt'],
    ['foo.txt', 'foo.txt'],
    ['', ''],
    ['/', ''],
    ['//', '']
  ]).forEach((expected, input) => {
    expect(trimLeadingSlashes(input)).toBe(expected)
  })
})

test('withLeadingSlash', () => {
  new Map([
    ['foo.txt', '/foo.txt'],
    ['/foo.txt', '/foo.txt'],
    ['', '/'],
    ['/', '/']
  ]).forEach((expected, input) => {
    expect(withLeadingSlash(input)).toBe(expected)
  })
})
