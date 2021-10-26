import { fireEvent } from '@testing-library/dom'
import { DataNode } from 'rc-tree/lib/interface'
import { act } from 'react-dom/test-utils'
import { render, waitForTime } from '../../../services/testing'
import { noop } from '../../../services/util'
import FileTreeNameInput, {
  createNameInputNode,
  getParentPathForNameInput,
  InputType,
  insertNameInputNodeIntoTree,
  openNameInput,
  TREE_INPUT_KEY
} from './FileTreeNameInput'

describe('<FileTreeNameInput />', () => {
  test('getParentPathForNameInput', () => {
    expect(getParentPathForNameInput()).toBe('/')
    expect(
      getParentPathForNameInput({ path: 'foo/bar/foo.txt', isFile: true })
    ).toBe('foo/bar')
    expect(
      getParentPathForNameInput({ path: 'foo/bar/dir', isFile: false })
    ).toBe('foo/bar/dir')
  })

  test('createNameInputNode', () => {
    const node = createNameInputNode(noop, noop, InputType.ADD_FILE)

    const { container } = render(<>{node.title}</>)

    expect(node.key).toBe(TREE_INPUT_KEY)
    expect(container.getElementsByTagName('input')).toHaveLength(1)
  })

  test('insertNameInputNodeIntoTree', () => {
    const tree: DataNode[] = [{ key: 'foo' }]
    const node = createNameInputNode(noop, noop, InputType.ADD_FILE)

    // node is inserted as first element into array
    expect(insertNameInputNodeIntoTree(tree, node, '/')).toStrictEqual([
      node,
      ...tree
    ])
    expect(insertNameInputNodeIntoTree(tree, node, 'foo')).toStrictEqual([
      {
        ...tree[0],
        children: [node]
      }
    ])
  })

  test('openNameInput', async () => {
    const tree: DataNode[] = [{ key: 'foo' }]
    const node = createNameInputNode(noop, noop, InputType.ADD_FILE)

    const nodeAtRoot = await openNameInput(
      noop,
      noop,
      InputType.ADD_FILE,
      tree,
      '/'
    )
    const nodeInChildren = await openNameInput(
      noop,
      noop,
      InputType.ADD_FILE,
      tree,
      'foo'
    )
    const nodeWithNewParents = await openNameInput(
      noop,
      noop,
      InputType.ADD_FILE,
      tree,
      'bar'
    )

    // node is inserted as first element into array
    expect(nodeAtRoot).toStrictEqual([node, ...tree])
    expect(nodeInChildren).toStrictEqual([
      {
        ...tree[0],
        children: [node]
      }
    ])
    expect(nodeWithNewParents).toStrictEqual([
      ...tree,
      expect.objectContaining({
        key: '/bar',
        children: [node]
      })
    ])
  })

  it('renders an <input /> element', () => {
    const { container } = render(<FileTreeNameInput />)

    expect(container.getElementsByTagName('input')).toHaveLength(1)
  })

  it('calls onConfirm() when Enter is pressend', async () => {
    const onConfirm = jest.fn()

    const { container } = render(<FileTreeNameInput onConfirm={onConfirm} />)

    const inputElement = container.getElementsByTagName('input')[0]

    await act(async () => {
      fireEvent.keyDown(inputElement, {
        key: 'Enter'
      })

      await waitForTime(3)
    })

    expect(onConfirm).toHaveBeenCalled()
  })

  it('calls onCancel() when Escape is pressend', async () => {
    const onCancel = jest.fn()

    const { container } = render(<FileTreeNameInput onCancel={onCancel} />)

    const inputElement = container.getElementsByTagName('input')[0]

    await act(async () => {
      fireEvent.keyDown(inputElement, {
        key: 'Escape'
      })

      await waitForTime(3)
    })

    expect(onCancel).toHaveBeenCalled()
  })
})
