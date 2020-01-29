import { Card } from 'antd'
import React from 'react'
import AceEditor from 'react-ace'
import { IMarker } from 'react-ace/src/types'
import { useGetAnswerFileQuery } from '../generated/graphql'
import AsyncPlaceholder from './AsyncContainer'

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

  const { content } = result.data.answerFile
  let value = content
  let firstLineNumber = 1
  const markers: IMarker[] = []
  if (lineStart) {
    value = cropToLines(
      content,
      lineStart,
      lineEnd || lineStart,
      numContextRows
    )
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
    <Card title={path} size="small" className="code-viewer-card">
      <AceEditor
        fontSize={14}
        className="code-viewer"
        readOnly
        showPrintMargin={false}
        maxLines={numberOfLines(value)}
        value={value}
        setOptions={{
          firstLineNumber,
          highlightActiveLine: false,
          highlightGutterLine: false
        }}
        width="100%"
        markers={markers}
      />
    </Card>
  )
}

export default CodeViewer
