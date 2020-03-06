// We could also import ext-modelist after AceEditor but eslint will sort them
// in the wrong order. This is why we import ace-builds explicitly below
import 'ace-builds'
import {
  getModeForPath,
  modesByName
} from 'ace-builds/src-noconflict/ext-modelist'
// warning: webpack-resolver increases compile time by a lot!
// an alternative is importing each mode and theme explicitly... but I am lazy
import 'ace-builds/webpack-resolver'
import { Card, Icon, Result } from 'antd'
import React from 'react'
import AceEditor from 'react-ace'
import { IMarker } from 'react-ace/src/types'
import { FileType, useGetAnswerFileQuery } from '../generated/graphql'
import { basename, isBinaryContent } from '../services/file'
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

const codeViewerMessage = (message: React.ReactNode) => {
  return (
    <Centered>
      <Result title={message} icon={<Icon type="file-unknown" />} />
    </Centered>
  )
}

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
    return codeViewerMessage(
      <>
        <code>${basename(path)}</code> is a {type.toLowerCase()}
      </>
    )
  }

  let value = content || ''

  if (isBinaryContent(value)) {
    return codeViewerMessage(
      <>
        <code>{basename(path)}</code> is a binary file
      </>
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

  const mode = getModeForPath(path) || modesByName.Text

  return (
    <AceEditor
      fontSize={14}
      className="code-viewer"
      readOnly
      mode={mode.name}
      theme="github"
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
