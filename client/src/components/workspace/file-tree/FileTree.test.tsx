import {
  NO_ANSWER_ID,
  NO_AUTH_TOKEN,
  NO_TASK_ID
} from '../../../hooks/workspace/useWorkspace'
import { mockFetch, render, waitForTime } from '../../../services/testing'
import { noop } from '../../../services/util'
import FileTree, {
  createDataNodesForDirectory,
  findNode,
  flattenNodes,
  insertDataNodes,
  isRoot,
  toDataNode
} from './FileTree'

describe('<FileTree />', () => {
  test('toDataNode', () => {
    expect(toDataNode({ path: '/foo.txt', size: 42 })).toStrictEqual({
      title: 'foo.txt',
      key: '/foo.txt',
      isLeaf: true
    })

    expect(toDataNode({ path: 'foo.txt', size: 42 })).toStrictEqual({
      title: 'foo.txt',
      key: '/foo.txt',
      isLeaf: true
    })

    expect(toDataNode({ path: '/foo/bar' })).toStrictEqual({
      title: 'bar',
      key: '/foo/bar',
      isLeaf: false
    })

    expect(toDataNode({ path: 'foo/bar' })).toStrictEqual({
      title: 'bar',
      key: '/foo/bar',
      isLeaf: false
    })
  })

  test('createDataNodesForDirectory', () => {
    expect(createDataNodesForDirectory('/foo/bar/test')).toStrictEqual(
      expect.objectContaining({
        key: '/foo',
        title: 'foo',
        isLeaf: false,
        children: [
          {
            key: '/foo/bar',
            title: 'bar',
            isLeaf: false,
            children: [
              {
                key: '/foo/bar/test',
                title: 'test',
                isLeaf: false
              }
            ]
          }
        ]
      })
    )

    expect(createDataNodesForDirectory('foo/bar/test')).toStrictEqual(
      expect.objectContaining({
        key: '/foo',
        title: 'foo',
        isLeaf: false,
        children: [
          {
            key: '/foo/bar',
            title: 'bar',
            isLeaf: false,
            children: [
              {
                key: '/foo/bar/test',
                title: 'test',
                isLeaf: false
              }
            ]
          }
        ]
      })
    )

    expect(createDataNodesForDirectory('/foo/bar/test', '/foo')).toStrictEqual(
      expect.objectContaining({
        key: '/foo/bar',
        title: 'bar',
        isLeaf: false,
        children: [
          {
            key: '/foo/bar/test',
            title: 'test',
            isLeaf: false
          }
        ]
      })
    )

    expect(createDataNodesForDirectory('foo/bar/test', 'foo')).toStrictEqual(
      expect.objectContaining({
        key: '/foo/bar',
        title: 'bar',
        isLeaf: false,
        children: [
          {
            key: '/foo/bar/test',
            title: 'test',
            isLeaf: false
          }
        ]
      })
    )
  })

  test('insertDataNodes', () => {
    const existingNodes = [
      {
        key: '/foo',
        title: 'foo',
        isLeaf: false,
        children: [
          {
            key: '/foo/bar',
            title: 'bar',
            isLeaf: false,
            children: [
              {
                key: '/foo/bar/test',
                title: 'test',
                isLeaf: false
              }
            ]
          }
        ]
      },
      {
        key: '/bar',
        title: 'bar',
        isLeaf: false,
        children: [
          {
            key: '/bar/foo',
            title: 'foo',
            isLeaf: false
          }
        ]
      }
    ]

    const newNodes = [
      {
        key: '/foo/bar/test.txt',
        title: 'test.txt',
        isLeaf: true
      },
      {
        key: '/foo/bar/baz',
        title: 'baz',
        isLeaf: false
      }
    ]

    expect(insertDataNodes('/foo/bar', existingNodes, newNodes)).toStrictEqual([
      {
        key: '/foo',
        title: 'foo',
        isLeaf: false,
        children: [
          {
            key: '/foo/bar',
            title: 'bar',
            isLeaf: false,
            children: newNodes
          }
        ]
      },
      {
        key: '/bar',
        title: 'bar',
        isLeaf: false,
        children: [
          {
            key: '/bar/foo',
            title: 'foo',
            isLeaf: false
          }
        ]
      }
    ])

    expect(insertDataNodes('/', existingNodes, newNodes)).toStrictEqual([
      ...existingNodes,
      ...newNodes
    ])
  })

  test('isRoot', () => {
    expect(isRoot('/')).toBe(true)
    expect(isRoot('')).toBe(true)
    expect(isRoot('/foo')).toBe(false)
    expect(isRoot('foo')).toBe(false)
  })

  test('flattenNodes', () => {
    expect(
      flattenNodes([
        {
          key: '/foo',
          title: 'foo',
          isLeaf: false
        },
        {
          key: '/bar',
          title: 'bar',
          isLeaf: false
        }
      ])
    ).toStrictEqual([
      {
        key: '/foo',
        title: 'foo',
        isLeaf: false
      },
      {
        key: '/bar',
        title: 'bar',
        isLeaf: false
      }
    ])

    expect(
      flattenNodes([
        {
          key: '/foo',
          title: 'foo',
          isLeaf: false,
          children: [
            {
              key: '/foo/bar',
              title: 'bar',
              isLeaf: false
            },
            {
              key: '/foo/baz',
              title: 'baz',
              isLeaf: false
            }
          ]
        },
        {
          key: '/bar',
          title: 'bar',
          isLeaf: false
        }
      ])
    ).toStrictEqual([
      {
        key: '/foo/bar',
        title: 'bar',
        isLeaf: false
      },
      {
        key: '/foo/baz',
        title: 'baz',
        isLeaf: false
      },
      {
        key: '/foo',
        title: 'foo',
        isLeaf: false,
        children: [
          {
            key: '/foo/bar',
            title: 'bar',
            isLeaf: false
          },
          {
            key: '/foo/baz',
            title: 'baz',
            isLeaf: false
          }
        ]
      },
      {
        key: '/bar',
        title: 'bar',
        isLeaf: false
      }
    ])
  })

  test('findNode', () => {
    const fooBar = {
      key: '/foo/bar',
      title: 'bar',
      isLeaf: false
    }

    const foo = {
      key: '/foo',
      title: 'foo',
      isLeaf: false,
      children: [
        fooBar,
        {
          key: '/foo/baz',
          title: 'baz',
          isLeaf: false
        }
      ]
    }

    const nodes = [
      foo,
      {
        key: '/bar',
        title: 'bar',
        isLeaf: false
      }
    ]

    expect(findNode(nodes, '/foo/bar')).toStrictEqual(fooBar)

    expect(findNode(nodes, '/foo')).toStrictEqual(foo)

    expect(findNode(nodes, '/baz')).toBeUndefined()
  })

  it('renders a workspace-file-tree', async () => {
    mockFetch()

    const baseUrl = 'https://codefreak.test'

    const workspaceContext = {
      isAvailable: true,
      baseUrl,
      answerId: NO_ANSWER_ID,
      authToken: NO_AUTH_TOKEN,
      taskId: NO_TASK_ID
    }

    const { container } = render(
      <FileTree onOpenFile={noop} />,
      {},
      { withWorkspaceContextProvider: true, workspaceContext }
    )

    await waitForTime()

    expect(
      container.getElementsByClassName('workspace-file-tree')
    ).toHaveLength(1)
  })
})
