import React from 'react'
import { FileContext } from '../generated/graphql'
import { Typography } from 'antd'

const { Text } = Typography

interface FileContextProps {
  context: FileContext
}

const FileContextTag: React.FC<FileContextProps> = props => {
  const { context } = props

  let text = context.path
  if (context.lineStart) {
    text += `:${context.lineStart}`
  }
  if (context.lineEnd) {
    text += `-${context.lineEnd}`
  }

  return <Text code>{text}</Text>
}

export default FileContextTag
