import {
  FileAddOutlined,
  FileOutlined,
  FolderAddOutlined,
  FolderOutlined
} from '@ant-design/icons'
import { Input, InputProps } from 'antd'
import { DataNode } from 'antd/lib/tree'
import { dirname } from 'path'
import { KeyboardEventHandler } from 'react'
import { noop } from '../../services/util'
import { findNode, insertDataNodes, isRoot, RightClickedItem } from './FileTree'

export enum InputType {
  ADD_FILE,
  ADD_DIRECTORY,
  RENAME_FILE,
  RENAME_DIRECTORY
}

/**
 * Unique key for file-name inputs in the file-tree
 */
export const TREE_INPUT_KEY = '\0'

/**
 * Returns the parent-path of the right-clicked-item to be used for adding a name-input to it.
 * If the right-clicked-item is a directory it is returned directly, otherwise it's actual parent directory is returned.
 *
 * @param rightClickedItem the right-clicked-item
 * @returns the parent-path of the right-clicked-item to be used for adding a name-input to it
 */
export const getParentPathForNameInput = (
  rightClickedItem?: RightClickedItem
): string => {
  if (rightClickedItem === undefined) {
    return '/'
  } else {
    return rightClickedItem.isFile
      ? dirname(rightClickedItem.path)
      : rightClickedItem.path
  }
}

/**
 * Returns an icon for an InputType.
 *
 * @param type the type of input
 * @returns the corresponding add-icon
 */
const getInputIcon = (type: InputType): React.ReactNode => {
  switch (type) {
    case InputType.ADD_FILE:
      return <FileAddOutlined />
    case InputType.ADD_DIRECTORY:
      return <FolderAddOutlined />
    case InputType.RENAME_FILE:
      return <FileOutlined />
    case InputType.RENAME_DIRECTORY:
      return <FolderOutlined />
  }
}

/**
 * Creates a DataNode for a name-input.
 *
 * @param onConfirm called when the input is confirmed
 * @param onCancel called when the input is cancelled
 * @param type the type of input to create
 * @returns a DataNode for the name-input with the given callbacks
 */
const createNameInputNode = (
  onConfirm: (name: string) => void,
  onCancel: () => void,
  type: InputType
): DataNode => {
  return {
    key: TREE_INPUT_KEY,
    title: <FileTreeNameInput onConfirm={onConfirm} onCancel={onCancel} />,
    icon: getInputIcon(type)
  }
}

/**
 * Inserts a given DataNode for a name-input into a tree of DataNodes as a child of the the given path.
 *
 * @param treeData the tree to insert the node into
 * @param nameInputNode the node to insert
 * @param parentPath the path which will be the parent of the node
 * @returns the new tree with the node inserted
 */
const insertNameInputNodeIntoTree = (
  treeData: DataNode[],
  nameInputNode: DataNode,
  parentPath: string
): DataNode[] => {
  const parentNode = findNode(treeData, parentPath)

  return isRoot(parentPath)
    ? [nameInputNode, ...treeData]
    : insertDataNodes(parentPath, treeData, [
        nameInputNode,
        ...(parentNode?.children ?? [])
      ])
}

/**
 * Opens a name-input as a DataNode inside the provided tree
 *
 * @param onConfirm triggered when the input is confirmed
 * @param onCancel triggered when the input is cancelled
 * @param type the type of input to create
 * @param treeData the tree to create the input in
 * @param path the path to create the input in
 * @returns a Promise that resolves to the tree with the name-input-node inserted
 */
export const openNameInput = async (
  onConfirm: (name: string) => void,
  onCancel: () => void,
  type: InputType,
  treeData: DataNode[] = [],
  path = '/'
): Promise<DataNode[]> => {
  const nameInputNode = createNameInputNode(onConfirm, onCancel, type)

  return insertNameInputNodeIntoTree(treeData, nameInputNode, path)
}

/**
 * Extends the default InputProps with callbacks for when the input was confirmed/cancelled
 */
interface FileTreeNameInputProps extends InputProps {
  /**
   * Callback when the input is cancelled (e.g. 'Escape'-key pressed)
   */
  onCancel?: () => void

  /**
   * Callback when the input was confirmed (e.g. 'Enter'-key pressed)
   *
   * @param name the name that was put in
   */
  onConfirm?: (name: string) => void
}
/**
 * A small input field which offers callbacks when the input was cancelled (escape-key) or confirmed (enter-key)
 */
const FileTreeNameInput = ({
  onCancel = noop,
  onConfirm = noop,
  ...inputProps
}: FileTreeNameInputProps) => {
  const handleKeyPress: KeyboardEventHandler<HTMLInputElement> = event => {
    if (event.key === 'Escape') {
      onCancel()
    } else if (event.key === 'Enter') {
      onConfirm(event.currentTarget.value)
    }
  }

  return (
    <Input {...inputProps} size="small" onKeyDown={handleKeyPress} autoFocus />
  )
}

export default FileTreeNameInput
