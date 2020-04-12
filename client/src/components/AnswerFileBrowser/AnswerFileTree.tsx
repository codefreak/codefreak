import { Tree } from 'antd'
import React from 'react'
import { useGetAnswerFileListQuery } from '../../services/codefreak-api'
import {
  fileListToTree,
  FileTreeDirectoryNode,
  FileTreeNode
} from '../../services/file'
import AsyncPlaceholder from '../AsyncContainer'
import { basename } from 'path'

const { TreeNode, DirectoryTree } = Tree

const renderTreeNodeRecursive = (
  node: FileTreeNode | FileTreeDirectoryNode
) => {
  let filename = basename(node.path)
  if (filename === '.') {
    filename = '/'
  }

  if (!('children' in node) || node.children === undefined) {
    return <TreeNode title={filename} key={node.path} isLeaf />
  }

  return (
    <TreeNode title={filename} key={node.path}>
      {node.children.map(renderTreeNodeRecursive)}
    </TreeNode>
  )
}

interface AnswerFileTreeProps {
  answerId: string
  onFileSelect?: (selectedNode: FileTreeNode | undefined) => void
}

const AnswerFileTree: React.FC<AnswerFileTreeProps> = ({
  answerId,
  onFileSelect
}) => {
  const result = useGetAnswerFileListQuery({ variables: { id: answerId } })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { answerFiles } = result.data
  const rootNode = fileListToTree(answerFiles)
  if (!rootNode) {
    return <>??</>
  }

  const onSelect = (selectedKeys: string[]) => {
    const path = selectedKeys.shift()
    if (path && onFileSelect) {
      onFileSelect(answerFiles.find(file => file.path === path))
    }
  }

  return (
    <DirectoryTree
      onSelect={onSelect}
      defaultExpandedKeys={[rootNode.path]}
      className="answer-file-tree"
    >
      {renderTreeNodeRecursive(rootNode)}
    </DirectoryTree>
  )
}

export default AnswerFileTree
