import { fileTree, sortTree, TreeSortFunction } from './file-tree'

interface DummyNode {
  path: string
}

const node = (path: string) => ({ path })
const nodes = (...paths: string[]): DummyNode[] => paths.map(node)
const virtualTreeNode = <T>(path: string, ...children: T[]) =>
  Object.assign(node(path), children.length ? { children } : {})
const treeNode = <T>(path: string, ...children: T[]) => ({
  ...virtualTreeNode(path, ...children),
  originalNode: node(path)
})

describe('fileListToTree', () => {
  it('returns empty array for empty list', () => {
    expect(fileTree([])).toEqual([])
  })

  it('nests children correctly', () => {
    expect(
      fileTree(nodes('/', '/a', '/b', '/b/c', '/b/c/d', '/b/b', '/c'))
    ).toEqual([
      treeNode(
        '/',
        treeNode('/a'),
        treeNode('/b', treeNode('/b/c', treeNode('/b/c/d')), treeNode('/b/b')),
        treeNode('/c')
      )
    ])
  })

  it('adds missing directories', () => {
    expect(fileTree(nodes('/a/b/c', '/b'))).toEqual([
      virtualTreeNode(
        '/',
        virtualTreeNode('/a', virtualTreeNode('/a/b', treeNode('/a/b/c'))),
        treeNode('/b')
      )
    ])
  })

  it('override virtual directories by actual ones', () => {
    expect(fileTree(nodes('/a/b/c', '/a', '/b'))).toEqual([
      virtualTreeNode(
        '/',
        treeNode('/a', virtualTreeNode('/a/b', treeNode('/a/b/c'))),
        treeNode('/b')
      )
    ])
  })

  it('preserves original path names', () => {
    expect(fileTree(nodes('./', './a', 'a/b', '/c'))).toEqual([
      treeNode('./', treeNode('./a', treeNode('a/b')), treeNode('/c'))
    ])
  })

  it('creates dot root node if no root is available', () => {
    expect(fileTree(nodes('a', 'a/b', 'b'))).toEqual([
      virtualTreeNode('.', treeNode('a', treeNode('a/b')), treeNode('b'))
    ])
  })
})

describe('sortTree', () => {
  it('sorts children alphabetically by path', () => {
    expect(
      sortTree([
        treeNode('z'),
        treeNode(
          'a',
          treeNode('b', treeNode('z'), treeNode('a'), treeNode('k'))
        )
      ])
    ).toEqual([
      treeNode('a', treeNode('b', treeNode('a'), treeNode('k'), treeNode('z'))),
      treeNode('z')
    ])
  })

  it('allows to pass a custom sort function', () => {
    // sort reverse by path
    const sortFunction: TreeSortFunction<DummyNode> = (a, b) =>
      b.path.localeCompare(a.path)

    expect(
      sortTree(
        [
          treeNode(
            'a',
            treeNode('b', treeNode('a'), treeNode('z'), treeNode('k'))
          ),
          treeNode('z')
        ],
        sortFunction
      )
    ).toEqual([
      treeNode('z'),
      treeNode('a', treeNode('b', treeNode('z'), treeNode('k'), treeNode('a')))
    ])
  })

  it('default sorter ignores leading dots and slashes for comparision', () => {
    expect(sortTree([treeNode('z'), treeNode('./b'), treeNode('/a')])).toEqual([
      treeNode('/a'),
      treeNode('./b'),
      treeNode('z')
    ])
  })
})
