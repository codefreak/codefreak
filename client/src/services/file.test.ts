import { File, FileType } from '../generated/graphql'
import { abspath, fileListToTree, numberOfLines, sliceLines } from './file'

const cid = { collectionId: '123', collectionDigest: 'fff' }
const file = { ...cid, type: FileType.File }
const dir = { ...cid, type: FileType.Directory }

test('abspath adds leading slash', () => {
  expect(abspath('./a/')).toBe('/a')
  expect(abspath('./')).toBe('/')
  expect(abspath('a')).toBe('/a')
  expect(abspath('/a/b')).toBe('/a/b')
})

describe('fileListToTree', () => {
  test('children get nested correctly', () => {
    const root: File = { ...dir, path: './' }
    const file1: File = { ...file, path: './a' }
    const dir1: File = { ...dir, path: './b' }
    const dir2: File = { ...dir, path: './aa' }
    const file2: File = { ...file, path: './b/b' }

    const rootNode = fileListToTree([root, file1, file2, dir1, dir2])
    expect(rootNode).toBeDefined()
    if (rootNode) {
      expect(rootNode.path).toBe('./')
      expect(rootNode).toHaveProperty('children')
      if ('children' in rootNode && rootNode.children) {
        expect(rootNode.children).toHaveLength(3)
        expect(rootNode.children[0].path).toBe(file1.path)
        expect(rootNode.children[1].path).toBe(dir2.path)
        expect(rootNode.children[1]).toHaveProperty('children')
        expect(rootNode.children[2].path).toBe(dir1.path)
        expect(rootNode.children[2]).toHaveProperty('children')
        if (
          'children' in rootNode.children[2] &&
          rootNode.children[2].children
        ) {
          expect(rootNode.children[2].children).toHaveLength(1)
          expect(rootNode.children[2].children[0].path).toBe(file2.path)
        }
      }
    }
  })

  test('creates missing parent dirs', () => {
    // misses root directory
    const dir1: File = { ...dir, path: 'bin/' }
    const dir1file1: File = { ...file, path: 'bin/file' }
    const dir2: File = { ...dir, path: 'aa' }

    const rootNode = fileListToTree([dir1, dir1file1, dir2])

    expect(rootNode).toBeDefined()
    if (rootNode) {
      expect(rootNode.path).toBe('/')
      expect(rootNode).toHaveProperty('children')
      if ('children' in rootNode && rootNode.children) {
        expect(rootNode.children).toHaveLength(2)
      }
    }
  })
})

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
