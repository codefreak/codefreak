import { Tabs, Tree } from 'antd'
import { DataNode, EventDataNode } from 'antd/lib/tree'
import useListWorkspaceFilesQuery, {
  FileSystemNode,
  listFiles
} from '../../hooks/workspace/useListWorkspaceFilesQuery'
import { useEffect, useState } from 'react'
import useWorkspace from '../../hooks/workspace/useWorkspace'
import TabPanel from './TabPanel'

const { DirectoryTree } = Tree

const convertToTreeData = (fileSystemNode: FileSystemNode) => ({
  title: fileSystemNode.path.substring(
    fileSystemNode.path.lastIndexOf('/') + 1
  ),
  key: fileSystemNode.path,
  isLeaf: fileSystemNode.size !== undefined ? true : undefined
})

interface FileTreeProps {
  onOpenFile: (path: string) => void
}

type Key = string | number

type OnSelectInfo = { node: EventDataNode }

const FileTree = ({ onOpenFile }: FileTreeProps) => {
  const { graphqlWebSocketClient, isAvailable } = useWorkspace()
  const { data } = useListWorkspaceFilesQuery()
  const [treeData, setTreeData] = useState<DataNode[]>()

  useEffect(() => {
    if (!treeData && data) {
      const loadedData = data.map(convertToTreeData)
      setTreeData(loadedData)
    }
  }, [data, treeData])

  const loadData = (treeNode: EventDataNode) => {
    return graphqlWebSocketClient
      ? listFiles(treeNode.key.toString(), graphqlWebSocketClient).then(
          treeNodes => {
            const convertedNodes = treeNodes.map(convertToTreeData)
            setTreeData(prevState =>
              prevState?.map(node => {
                if (node.key === treeNode.key) {
                  return {
                    ...node,
                    children: convertedNodes
                  }
                }
                return node
              })
            )
          }
        )
      : Promise.reject('No graphql websocket client found')
  }

  const handleSelect = (_: Key[], { node }: OnSelectInfo) => {
    if (node.isLeaf) {
      onOpenFile(node.key.toString())
    }
  }

  return (
    <Tabs type="card" className="workspace-tabs workspace-file-tree">
      <Tabs.TabPane tab="Files">
        <TabPanel withPadding loading={!isAvailable}>
          <DirectoryTree
            treeData={treeData}
            loadData={loadData}
            onSelect={handleSelect}
          />
        </TabPanel>
      </Tabs.TabPane>
    </Tabs>
  )
}

export default FileTree
