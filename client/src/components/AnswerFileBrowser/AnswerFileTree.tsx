import { Tree } from 'antd'
import { relative, resolve } from 'path'
import React from 'react'
import {
  FileType,
  GetAnswerFileListQueryResult,
  useGetAnswerFileListQuery
} from '../../services/codefreak-api'
import {
  fileTree,
  sortTree,
  TreeNode as TreeNodeModel
} from '../../services/file-tree'
import AsyncPlaceholder from '../AsyncContainer'

const { TreeNode, DirectoryTree } = Tree

export type AnswerFile = NonNullable<
  GetAnswerFileListQueryResult['data']
>['answerFiles'][number]

const isRootDir = (path: string) => {
  return resolve('/', path) === '/'
}

const renderTreeNodeRecursive = (
  node: TreeNodeModel<AnswerFile>,
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
  answerId: string
  onFileSelect?: (selectedNode: AnswerFile) => void
}

const AnswerFileTree: React.FC<AnswerFileTreeProps> = ({
  answerId,
  onFileSelect
}) => {
  const result = useGetAnswerFileListQuery({
    variables: { id: answerId }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { answerFiles } = result.data
  let rootNodes = sortTree(fileTree(answerFiles))

  // omit the root node and render its children
  if (rootNodes.length === 1 && isRootDir(rootNodes[0].path)) {
    rootNodes = rootNodes[0].children || []
  }

  const onSelect = (selectedKeys: string[]) => {
    if (!onFileSelect) {
      return
    }
    const path = selectedKeys.shift()
    const selectedFile = path && answerFiles.find(file => file.path === path)
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
