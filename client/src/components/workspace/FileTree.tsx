import {
  Dropdown,
  Input,
  InputProps,
  Menu,
  Modal,
  Tabs,
  Tree,
  TreeProps
} from 'antd'
import { DataNode, EventDataNode } from 'antd/lib/tree'
import useListWorkspaceFilesQuery, {
  FileSystemNode,
  listFiles
} from '../../hooks/workspace/useListWorkspaceFilesQuery'
import { KeyboardEventHandler, useEffect, useState } from 'react'
import useWorkspace from '../../hooks/workspace/useWorkspace'
import TabPanel from './TabPanel'
import {
  DeleteOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  FileAddOutlined,
  FolderAddOutlined
} from '@ant-design/icons'
import { dirname } from 'path'
import useCreateWorkspacePathMutation, {
  PathType
} from '../../hooks/workspace/useCreateWorkspacePathMutation'
import { trimLeadingSlashes, withTrailingSlash } from '../../services/strings'
import { noop } from '../../services/util'
import useDeleteWorkspacePathMutation from '../../hooks/workspace/useDeleteWorkspacePathMutation'
import { messageService } from '../../services/message'

const { DirectoryTree } = Tree
const { confirm } = Modal

const TREE_INPUT_KEY = '\0'

const convertToTreeData = (fileSystemNode: FileSystemNode) => ({
  title: fileSystemNode.path.substring(
    fileSystemNode.path.lastIndexOf('/') + 1
  ),
  key: fileSystemNode.path,
  isLeaf: fileSystemNode.size !== undefined ? true : undefined
})

const combineLoadedNodesAndExistingChildren = (
  loadedNodes: DataNode[],
  children: DataNode[] = []
) =>
  loadedNodes.map(node => {
    const existingNode = children.find(child => child.key === node.key)

    return existingNode
      ? {
          ...node,
          children: existingNode.children
        }
      : node
  })

const insertLoadedNodes = (
  path: string,
  nodes: DataNode[],
  loadedNodes: DataNode[]
): DataNode[] =>
  nodes.map(node => {
    if (node.key === path) {
      return {
        ...node,
        children: combineLoadedNodesAndExistingChildren(
          loadedNodes,
          node.children
        )
      }
    } else if (node.children !== undefined) {
      return {
        ...node,
        children: insertLoadedNodes(path, node.children, loadedNodes)
      }
    }

    return node
  })

const isRoot = (path: string) => path === '/' || path === ''

const flattenNodes = (nodes: DataNode[]): DataNode[] => {
  return nodes.flatMap(node => {
    const children = flattenNodes(node.children ?? [])
    return [...children, node].flat()
  })
}

const findNode = (nodes: DataNode[], path: string) =>
  flattenNodes(nodes).find(node => node.key === path)

interface TreeInputProps extends InputProps {
  onCancel?: () => void
  onConfirm?: (name: string) => void
}

const TreeInput = ({
  onCancel = noop,
  onConfirm = noop,
  ...inputProps
}: TreeInputProps) => {
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

interface FileTreeProps {
  onOpenFile: (path: string) => void
}

type RightClickedItem = {
  path: string
  isFile: boolean
}

const FileTree = ({ onOpenFile }: FileTreeProps) => {
  const { graphqlWebSocketClient, isAvailable } = useWorkspace()
  const { data } = useListWorkspaceFilesQuery()
  const { mutate: createPath } = useCreateWorkspacePathMutation()
  const { mutate: deletePath } = useDeleteWorkspacePathMutation()
  const [treeData, setTreeData] = useState<DataNode[]>()
  const [rightClickedItem, setRightClickedItem] = useState<RightClickedItem>()
  const [expandedKeys, setExpandedKeys] = useState<string[]>([])

  useEffect(() => {
    if (!treeData && data) {
      const loadedData = data.map(convertToTreeData)
      setTreeData(loadedData)
    }
  }, [data, treeData])

  const loadData = async (path: string) => {
    return graphqlWebSocketClient
      ? listFiles(path, graphqlWebSocketClient).then(treeNodes => {
          const convertedNodes = treeNodes.map(convertToTreeData)
          setTreeData(prevState =>
            isRoot(path)
              ? combineLoadedNodesAndExistingChildren(convertedNodes, prevState)
              : insertLoadedNodes(path, prevState ?? [], convertedNodes)
          )
        })
      : Promise.reject('No graphql websocket client found')
  }

  const loadDataFromTreeNode = (treeNode: EventDataNode) =>
    loadData(treeNode.key.toString())

  const handleSelect: TreeProps['onSelect'] = (_, { node }) => {
    if (node.isLeaf && node.key !== TREE_INPUT_KEY) {
      onOpenFile(node.key.toString())
    }
  }

  const handleRename = () => {
    if (!rightClickedItem) {
      return
    }

    // TODO open name-input in path
    // TODO rename file with name from input
    // TODO inside rename (for now): Error("Not implemented yet")
    // TODO error when name already exists
    throw new Error('Unsupported operation')
  }

  const handleDelete = () => {
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
        deletePath({ path }, { onSuccess: () => loadData(parentPath) })
      }
    })
  }

  const openNameInput = async (
    onConfirm: (name: string, parentPath: string) => void,
    type: PathType
  ) => {
    let parentPath: string

    if (rightClickedItem === undefined) {
      parentPath = '/'
    } else {
      parentPath = rightClickedItem.isFile
        ? dirname(rightClickedItem.path)
        : rightClickedItem.path
    }

    // TODO track an 'added' state
    // if === -1 => add expanded key and load data
    // if !== -1 => add input to tree data
    if (expandedKeys.indexOf(parentPath) === -1) {
      console.log({ treeData })
      await loadData(parentPath)
      setExpandedKeys(prevState => [parentPath, ...prevState])
      console.log({ treeData })
    }

    const handleInputConfirm = (fileName: string) => {
      const fullPath =
        withTrailingSlash(parentPath) + trimLeadingSlashes(fileName)
      if (findNode(treeData ?? [], fullPath) === undefined) {
        onConfirm(fileName, parentPath)
      } else {
        messageService.error(fileName + ' already exists')
      }
    }

    const handleInputCancel = () => loadData(parentPath)

    let icon

    switch (type) {
      case PathType.FILE:
        icon = <FileAddOutlined />
        break
      case PathType.DIRECTORY:
        icon = <FolderAddOutlined />
        break
    }

    const nameInputNode: DataNode = {
      key: TREE_INPUT_KEY,
      title: (
        <TreeInput
          onConfirm={handleInputConfirm}
          onCancel={handleInputCancel}
        />
      ),
      icon,
      isLeaf: type === PathType.FILE
    }

    const parentNode = findNode(treeData ?? [], parentPath)

    setTreeData(prevState =>
      isRoot(parentPath)
        ? [nameInputNode, ...(prevState ?? [])]
        : insertLoadedNodes(parentPath, prevState ?? [], [
            nameInputNode,
            ...(parentNode?.children ?? [])
          ])
    )
  }

  const handleAdd = (type: PathType) => () => {
    const handleNameConfirm = (fileName: string, parentPath: string) => {
      const path = withTrailingSlash(parentPath) + trimLeadingSlashes(fileName)

      createPath(
        { path, type },
        {
          onSuccess: () => loadData(parentPath)
        }
      )
    }

    openNameInput(handleNameConfirm, type)
  }

  const rightClickMenu = (
    <Menu>
      {rightClickedItem !== undefined
        ? [
            <Menu.Item icon={<EditOutlined />} onClick={handleRename} disabled>
              Rename
            </Menu.Item>,
            <Menu.Item
              icon={<DeleteOutlined />}
              style={{ color: 'red' }}
              onClick={handleDelete}
            >
              Delete
            </Menu.Item>,
            <Menu.Divider />
          ]
        : null}
      <Menu.Item icon={<FileAddOutlined />} onClick={handleAdd(PathType.FILE)}>
        Add file
      </Menu.Item>
      <Menu.Item
        icon={<FolderAddOutlined />}
        onClick={handleAdd(PathType.DIRECTORY)}
      >
        Add directory
      </Menu.Item>
    </Menu>
  )

  const handleRightClick: TreeProps['onRightClick'] = ({ node }) => {
    setRightClickedItem({
      path: node.key.toString(),
      isFile: node.isLeaf ?? false
    })
  }

  // we manage the expanded props manually so we can expand directories if new nodes are created inside it
  const handleExpand: TreeProps['onExpand'] = newExpandedKeys => {
    setExpandedKeys([...newExpandedKeys.map(e => e.toString())])
  }

  return (
    <Tabs type="card" className="workspace-tabs workspace-file-tree">
      <Tabs.TabPane tab="Files">
        <TabPanel withPadding loading={!isAvailable}>
          <Dropdown overlay={rightClickMenu} trigger={['contextMenu']}>
            <div style={{ height: '100%', width: '100%' }}>
              <DirectoryTree
                treeData={treeData}
                loadData={loadDataFromTreeNode}
                onSelect={handleSelect}
                onRightClick={handleRightClick}
                expandedKeys={expandedKeys}
                onExpand={handleExpand}
              />
            </div>
          </Dropdown>
        </TabPanel>
      </Tabs.TabPane>
    </Tabs>
  )
}

export default FileTree
