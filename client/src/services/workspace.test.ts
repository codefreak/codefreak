import { extractRelativeFilePath } from './workspace'

test('extractRelativeFilePath', () => {
  expect(extractRelativeFilePath('http://codefreak.test/files/file.txt')).toBe(
    'file.txt'
  )
  expect(
    extractRelativeFilePath('http://codefreak.test/files/src/main/main.js')
  ).toBe('src/main/main.js')
  try {
    extractRelativeFilePath('http://codefreak.test/file.txt')
    fail('Missing /files/ should throw')
  } catch (error) {}
})
