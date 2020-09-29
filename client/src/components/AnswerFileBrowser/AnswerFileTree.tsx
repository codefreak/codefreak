import { Tree } from 'antd'
import { relative, resolve } from 'path'
import React from 'react'
import { FileType } from '../../services/codefreak-api'
import {
  fileTree,
  sortTree,
  TreeNode as TreeNodeModel
} from '../../services/file-tree'

const { TreeNode, DirectoryTree } = Tree

export interface FileTreeFile {
  path: string
  type: FileType
}

const isRootDir = (path: string) => {
  return resolve('/', path) === '/'
}

const renderTreeNodeRecursive = (
  node: TreeNodeModel<FileTreeFile>,
  parentPath: string
) => {
  const filename = relative(parentPath, node.path)

  if (!node.children) {
    const isLeaf = node.originalNode && node.originalNode.type === FileType.File
    return <TreeNode title={filename} key={node.path} isLeaf={isLeaf} />
  }

  return (
    <TreeNode title={filename} key={node.path}>
      {node.children.map(child => renderTreeNodeRecursive(child, node.path))}
    </TreeNode>
  )
}

export interface AnswerFileTreeProps {
  onFileSelect?: (selectedNode: FileTreeFile) => void
  files: FileTreeFile[]
}

const AnswerFileTree: React.FC<AnswerFileTreeProps> = ({
  onFileSelect,
  files
}) => {
  let rootNodes = sortTree(fileTree(files))

  // omit the root node and render its children
  if (rootNodes.length === 1 && isRootDir(rootNodes[0].path)) {
    rootNodes = rootNodes[0].children || []
  }

  const onSelect = (selectedKeys: string[]) => {
    if (!onFileSelect) {
      return
    }
    const path = selectedKeys.shift()
    const selectedFile = path && files.find(file => file.path === path)
    if (selectedFile) {
      onFileSelect(selectedFile)
    }
  }

  return (
    <DirectoryTree
      onSelect={onSelect}
      defaultExpandedKeys={[]}
      className="answer-file-tree"
    >
      {rootNodes.map(node => renderTreeNodeRecursive(node, '/'))}
    </DirectoryTree>
  )
}

export default AnswerFileTree
