import { numberOfLines, sliceLines } from './file'

test('numberOfLines', () => {
  expect(numberOfLines('')).toBe(1)
  expect(numberOfLines('\r')).toBe(1)
  expect(numberOfLines('\n')).toBe(2)
  expect(numberOfLines('\r\n')).toBe(2)
  expect(numberOfLines('\n\r\n')).toBe(3)
})

test('sliceLines', () => {
  expect(sliceLines('a\nb\nc', 1, 2)).toBe('a\nb')
  expect(sliceLines('a\nb\nc', 2, 2)).toBe('b')
  expect(sliceLines('a\nb\nc', undefined, 2)).toBe('a\nb')
  expect(sliceLines('a\nb\nc', 2, undefined)).toBe('b\nc')
  expect(sliceLines('a\nb\nc', -10, 20)).toBe('a\nb\nc')
  expect(sliceLines('\n\n', 1, 1)).toBe('')
})
