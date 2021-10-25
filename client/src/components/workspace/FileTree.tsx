import { Dropdown, Modal, Tree, TreeProps } from 'antd'
import { DataNode, EventDataNode } from 'antd/lib/tree'
import useListWorkspaceFilesQuery, {
  FileSystemNode,
  listFiles
} from '../../hooks/workspace/useListWorkspaceFilesQuery'
import { useEffect, useState } from 'react'
import useWorkspace from '../../hooks/workspace/useWorkspace'
import { ExclamationCircleOutlined } from '@ant-design/icons'
import { basename, dirname } from 'path'
import useCreateWorkspacePathMutation, {
  PathType
} from '../../hooks/workspace/useCreateWorkspacePathMutation'
import { trimLeadingSlashes, withTrailingSlash } from '../../services/strings'
import useDeleteWorkspacePathMutation from '../../hooks/workspace/useDeleteWorkspacePathMutation'
import { WorkspaceTab, WorkspaceTabType } from '../../services/workspace-tabs'
import FileTreeRightClickMenu from './FileTreeRightClickMenu'
import {
  getParentPathForNameInput,
  InputType,
  openNameInput,
  TREE_INPUT_KEY
} from './FileTreeNameInput'
import TabPanel from './TabPanel'
import { messageService } from '../../services/message'
import useRenameWorkspacePathMutation from '../../hooks/workspace/useRenameWorkspacePathMutation'

const { DirectoryTree } = Tree
const { confirm } = Modal

/**
 * Converts a FileSystemNode to a DataNode
 *
 * @param fileSystemNode the node to convert
 * @returns the converted DataNode
 */
const toDataNode = (fileSystemNode: FileSystemNode): DataNode => ({
  title: basename(fileSystemNode.path),
  key: fileSystemNode.path,
  isLeaf: fileSystemNode.size !== undefined
})

/**
 * Combines newly loaded DataNodes with their previous data so that their loaded children are not lost when updated.
 *
 * Example:
 * The nodes in path `/foo` were loaded previously and their children were loaded too.
 * When re-loading the nodes in `/foo` the loaded nodes have no children (since the loading is only one level)
 * and the already loaded children would be overwritten by `undefined`.
 * includeExistingChildrenToNewlyLoadedDataNodes([...newNodes], [...oldNodes]) adds the children to the corresponding
 * newly loaded nodes.
 *
 * @param newlyLoadedNodes the DataNodes that were loaded (all with a common parent-path)
 * @param existingNodes the DataNodes that were previously loaded for the common parent-path
 * @returns the combined DataNodes
 */
const includeExistingChildrenToNewlyLoadedDataNodes = (
  newlyLoadedNodes: DataNode[],
  existingNodes: DataNode[] = []
) =>
  newlyLoadedNodes.map(node => {
    const children = existingNodes.find(
      existingNode => existingNode.key === node.key
    )?.children

    return children
      ? {
          ...node,
          children
        }
      : node
  })

/**
 * Creates a DataNode with parent-directories starting at `startingPath`
 *
 * @param path the path to create a DataNode for
 * @param startingPath the path to start creatign parent-directies as DataNodes
 * @returns a hierarchy of DataNodes bound through their children property
 */
const createDataNodesForDirectory = (path: string, startingPath = '/') => {
  let node: DataNode = {
    key: path,
    isLeaf: false
  }

  let currentPath = path

  while (currentPath !== startingPath) {
    currentPath = dirname(currentPath)

    node = {
      key: currentPath,
      isLeaf: false,
      children: [node]
    }
  }

  return node
}

/**
 * Recursively inserts new nodes into an existing tree of DataNodes at a given parent-path. If some of the new nodes already existed in the
 * old tree their children are kept and added to the corresponding new nodes.
 *
 * @param path the path were the new nodes are to be inserted as children
 * @param existingNodes the existing nodes to insert into
 * @param newNodes the nodes to be inserted
 * @returns the tree with the nodes inserted
 */
export const insertDataNodes = (
  path: string,
  existingNodes: DataNode[],
  newNodes: DataNode[]
): DataNode[] =>
  existingNodes.map(node => {
    if (node.key === path) {
      // Don't overwrite existing children of the nodes
      return {
        ...node,
        children: includeExistingChildrenToNewlyLoadedDataNodes(
          newNodes,
          node.children
        )
      }
    } else if (path.includes(node.key.toString())) {
      const parentNodes = node.children ?? [
        createDataNodesForDirectory(path, node.key.toString())
      ]

      return {
        ...node,
        children: insertDataNodes(path, parentNodes, newNodes)
      }
    }

    return node
  })

/**
 * Returns whether the given path can be interpreted as the root-directory '/'
 *
 * @param path the path
 * @returns whether the path can be interpreted as the root-directory '/'
 */
export const isRoot = (path: string) => path === '/' || path === ''

/**
 * Flattens a tree of nodes and its children
 *
 * @param nodes the tree of nodes to flatten
 * @returns the flattened tree
 */
const flattenNodes = (nodes: DataNode[]): DataNode[] => {
  return nodes.flatMap(node => {
    const children = flattenNodes(node.children ?? [])
    return [...children, node].flat()
  })
}

/**
 * Finds a DataNode in a tree of nodes by the given path
 *
 * @param nodes the nodes to search in
 * @param path the path of the node to find
 * @returns the node if found or undefinded otherwise
 */
export const findNode = (
  nodes: DataNode[],
  path: string
): DataNode | undefined => flattenNodes(nodes).find(node => node.key === path)

/**
 * Represents an item in the FileTree that was right-clicked
 */
export type RightClickedItem = {
  /**
   * The path of the right-clicked item
   */
  path: string
  /**
   * Whether the right-clicked item is a file
   */
  isFile: boolean
}

/**
 * Offers a callback when a file-entry in the tree is opened
 */
interface FileTreeProps {
  /**
   * A callback when a file-entry in the tree is opened
   *
   * @param path the path of the opened file
   */
  onOpenFile: (path: string) => void
}

/**
 * Renders the files and directories of the workspace in a tree format
 */
const FileTree = ({ onOpenFile }: FileTreeProps) => {
  const { graphqlWebSocketClient } = useWorkspace()
  const { data } = useListWorkspaceFilesQuery()
  const { mutate: createPath } = useCreateWorkspacePathMutation()
  const { mutate: deletePath } = useDeleteWorkspacePathMutation()
  const { mutate: renamePath } = useRenameWorkspacePathMutation()
  const [treeData, setTreeData] = useState<DataNode[]>()
  const [isRightClickMenuOpen, setIsRightClickMenuOpen] = useState(false)
  const [rightClickedItem, setRightClickedItem] = useState<RightClickedItem>()
  const [expandedKeys, setExpandedKeys] = useState<string[]>([])

  useEffect(() => {
    if (!treeData && data) {
      const loadedData = data.map(toDataNode)
      setTreeData(loadedData)
    }
  }, [data, treeData])

  useEffect(() => {
    if (!isRightClickMenuOpen && rightClickedItem !== undefined) {
      setRightClickedItem(undefined)
    }
  }, [isRightClickMenuOpen, rightClickedItem])

  const loadData = async (path: string) => {
    if (!graphqlWebSocketClient) {
      throw new Error('No graphql websocked client was found')
    }

    const treeNodes = await listFiles(path, graphqlWebSocketClient)

    return treeNodes.map(toDataNode)
  }

  const loadDataAndUpdateTree = async (path: string): Promise<void> => {
    if (!graphqlWebSocketClient) {
      return Promise.reject('No graphql websocket client found')
    }

    if (path === TREE_INPUT_KEY) {
      // When an input for a directory is opened there is no data to load
      return
    }

    const newData = await loadData(path)

    setTreeData(prevState =>
      isRoot(path)
        ? includeExistingChildrenToNewlyLoadedDataNodes(newData, prevState)
        : insertDataNodes(path, prevState ?? [], newData)
    )
  }

  const loadDataAndUpdateTreeFromTreeNode = async (
    treeNode: EventDataNode
  ): Promise<void> => loadDataAndUpdateTree(treeNode.key.toString())

  const openIfFile: TreeProps['onSelect'] = (_, { node }) => {
    if (node.isLeaf && node.key !== TREE_INPUT_KEY) {
      onOpenFile(node.key.toString())
    }
  }

  const deleteRightClickedItem = () => {
    setIsRightClickMenuOpen(false)

    if (!rightClickedItem) {
      return
    }

    const path = rightClickedItem.path
    const parentPath = dirname(path)

    confirm({
      title: `Are you sure you want to delete '${trimLeadingSlashes(path)}'?`,
      icon: <ExclamationCircleOutlined />,
      okText: 'Delete',
      okType: 'danger',
      onOk() {
        deletePath(
          { path },
          { onSuccess: () => loadDataAndUpdateTree(parentPath) }
        )
      }
    })
  }

  const openNameInputForType =
    (
      type: InputType,
      onNameConfirmed: (path: string, type: PathType) => void
    ) =>
    async () => {
      setIsRightClickMenuOpen(false)

      const parentPath = getParentPathForNameInput(rightClickedItem)

      const handleConfirm = async (fileName: string) => {
        if (!graphqlWebSocketClient) {
          return
        }

        const path =
          withTrailingSlash(parentPath) + trimLeadingSlashes(fileName)

        const tree = isRoot(parentPath)
          ? treeData ?? []
          : await loadData(parentPath)

        if (findNode(tree, path) === undefined) {
          const pathType =
            type === InputType.ADD_FILE ? PathType.FILE : PathType.DIRECTORY

          onNameConfirmed(path, pathType)
        } else {
          messageService.error(
            `${basename(path)} already exists in ${dirname(path)}`
          )
        }
      }

      const handleCancel = () => loadDataAndUpdateTree(parentPath)

      if (!expandedKeys.includes(parentPath) && !isRoot(parentPath)) {
        setExpandedKeys(prevState => [parentPath, ...prevState])
      }

      const newTreeData = await openNameInput(
        handleConfirm,
        handleCancel,
        type,
        treeData,
        parentPath
      )

      setTreeData(newTreeData)
    }

  const addPath = (path: string, type: PathType) => {
    const parentPath = dirname(path)
    createPath(
      { path, type },
      {
        onSuccess: () => loadDataAndUpdateTree(parentPath)
      }
    )
  }

  const renameRightClickedItem = () => {
    setIsRightClickMenuOpen(false)

    if (!rightClickedItem) {
      return
    }

    const handleNameConfirmed = (path: string) => {
      renamePath({ sourcePath: rightClickedItem.path, targetPath: path })
    }

    return rightClickedItem.isFile
      ? openNameInputForType(InputType.RENAME_FILE, handleNameConfirmed)
      : openNameInputForType(InputType.RENAME_DIRECTORY, handleNameConfirmed)
  }

  const rightClickMenu = (
    <FileTreeRightClickMenu
      rightClickedItem={rightClickedItem}
      onRename={renameRightClickedItem}
      onDelete={deleteRightClickedItem}
      onAddFile={openNameInputForType(InputType.ADD_FILE, addPath)}
      onAddDirectory={openNameInputForType(InputType.ADD_DIRECTORY, addPath)}
    />
  )

  const handleRightClick: TreeProps['onRightClick'] = ({ node }) => {
    setRightClickedItem({
      path: node.key.toString(),
      isFile: node.isLeaf ?? false
    })
  }

  // Manage the expanded keys manually so we can expand directories if new nodes are created inside it
  const handleExpand: TreeProps['onExpand'] = newExpandedKeys => {
    setExpandedKeys([...newExpandedKeys.map(e => e.toString())])
  }

  return (
    <TabPanel withPadding>
      <Dropdown
        overlay={rightClickMenu}
        trigger={['contextMenu']}
        onVisibleChange={setIsRightClickMenuOpen}
        visible={isRightClickMenuOpen}
      >
        <div className="workspace-file-tree">
          <DirectoryTree
            treeData={treeData}
            loadData={loadDataAndUpdateTreeFromTreeNode}
            onSelect={openIfFile}
            onRightClick={handleRightClick}
            expandedKeys={expandedKeys}
            onExpand={handleExpand}
          />
        </div>
      </Dropdown>
    </TabPanel>
  )
}

/**
 * Represents a WorkspaceTab that renders the files of a workspace in a FileTree.
 * A callback is needed for when a file is opened through the tree.
 *
 * @constructor
 */
export class FileTreeWorkspaceTab extends WorkspaceTab {
  private onOpenFile: FileTreeProps['onOpenFile']

  constructor(onOpenFile: FileTreeProps['onOpenFile']) {
    super(WorkspaceTabType.FILE_TREE, '')
    this.onOpenFile = onOpenFile
  }

  renderTitle() {
    return 'Files'
  }

  renderContent() {
    return <FileTree onOpenFile={this.onOpenFile} />
  }
}

export default FileTree
