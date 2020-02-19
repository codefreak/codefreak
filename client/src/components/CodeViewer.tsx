import { Card, Icon, Result } from 'antd'
import React from 'react'
import AceEditor from 'react-ace'
import { IMarker } from 'react-ace/src/types'
import { FileType, useGetAnswerFileQuery } from '../generated/graphql'
import { basename } from '../services/file'
import AsyncPlaceholder from './AsyncContainer'

import Centered from './Centered'
import './CodeViewer.less'

interface CodeViewerProps {
  answerId: string
  path: string
  lineStart?: number
  lineEnd?: number
  numContextRows?: number
}

const cropToLines = (
  content: string,
  lineStart: number,
  lineEnd: number,
  additionalContextLines: number
) => {
  const lines = content.split(/\r?\n/g)
  const start = Math.max(lineStart - additionalContextLines - 1, 0)
  const end = Math.min(lineEnd + additionalContextLines, lines.length)
  return lines.slice(start, end).join('\n')
}

const numberOfLines = (input: string) =>
  (input.match(/\r?\n/g) || []).length + 1

const CodeViewer: React.FC<CodeViewerProps> = ({
  answerId,
  path,
  lineStart,
  lineEnd,
  numContextRows = 2
}) => {
  const result = useGetAnswerFileQuery({ variables: { id: answerId, path } })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { content, type } = result.data.answerFile

  if (type !== FileType.File) {
    return (
      <Centered>
        <Result
          title={
            <>
              Can only view Files. <code>${basename(path)}</code> is a $
              {type.toLowerCase()}`
            </>
          }
          icon={<Icon type="file-unknown" />}
        />
      </Centered>
    )
  }

  let value = content || ''

  if (/[\x00-\x08\x0E-\x1F]/.test(value)) {
    return (
      <Centered>
        <Result
          title={
            <>
              <code>{basename(path)}</code> is a binary file
            </>
          }
          icon={<Icon type="file-unknown" />}
        />
      </Centered>
    )
  }

  let firstLineNumber = 1
  const markers: IMarker[] = []
  if (lineStart) {
    value = cropToLines(value, lineStart, lineEnd || lineStart, numContextRows)
    firstLineNumber = Math.max(lineStart - numContextRows, 1)
    markers.push({
      startRow: lineStart - firstLineNumber,
      startCol: 0,
      endRow: (lineEnd || lineStart) - firstLineNumber,
      endCol: Infinity,
      className: 'highlight-line',
      type: 'background'
    })
  }

  return (
    <AceEditor
      fontSize={14}
      className="code-viewer"
      readOnly
      showPrintMargin={false}
      maxLines={lineStart ? numberOfLines(value) : undefined}
      value={value}
      setOptions={{
        firstLineNumber,
        highlightActiveLine: false,
        highlightGutterLine: false
      }}
      width="100%"
      height="100%"
      markers={markers}
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
