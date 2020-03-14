import { Card, Icon, Result } from 'antd'
import { editor, Range } from 'monaco-editor'
import React from 'react'
import { FileType, useGetAnswerFileQuery } from '../generated/graphql'
import { basename, isBinaryContent } from '../services/file'
import AsyncPlaceholder from './AsyncContainer'
import Editor from './Editor'

import Centered from './Centered'
import './CodeViewer.less'

interface CodeViewerProps {
  answerId: string
  path: string
  lineStart?: number
  lineEnd?: number
  numContextRows?: number
}

const codeViewerMessage = (message: React.ReactNode) => {
  return (
    <Centered>
      <Result title={message} icon={<Icon type="file-unknown" />} />
    </Centered>
  )
}

const CodeViewer: React.FC<CodeViewerProps> = ({
  answerId,
  path: queryPath,
  lineStart,
  lineEnd,
  numContextRows = 4
}) => {
  const result = useGetAnswerFileQuery({
    variables: { id: answerId, path: queryPath }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  // use path from response or content and path can by out-of-sync
  const { content, type, path } = result.data.answerFile

  if (type !== FileType.File) {
    return codeViewerMessage(
      <>
        <code>${basename(path)}</code> is a {type.toLowerCase()}
      </>
    )
  }

  const value = content || ''

  if (isBinaryContent(value)) {
    return codeViewerMessage(
      <>
        <code>{basename(path)}</code> is a binary file
      </>
    )
  }

  const decorations: editor.IModelDeltaDecoration[] = []
  let maxNumLines
  if (lineStart) {
    decorations.push({
      range: new Range(lineStart, 1, lineEnd || lineStart, Infinity),
      options: {
        className: 'highlight-line'
      }
    })
    maxNumLines = numContextRows * 2
    maxNumLines += lineEnd ? lineEnd - lineStart : 1
  }

  return (
    <Editor
      readOnly
      currentLine={lineStart}
      maxNumLines={maxNumLines}
      value={value}
      path={`/${answerId}/${path}`}
      decorations={decorations}
    />
  )
}

export const CodeViewerCard: React.FC<CodeViewerProps> = props => {
  const { path } = props
  return (
    <Card title={path} size="small" className="code-viewer-card">
      <CodeViewer {...props} />
    </Card>
  )
}

export default CodeViewer
