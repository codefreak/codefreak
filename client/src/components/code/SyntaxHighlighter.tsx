import React from 'react'
import SyntaxHighlighterComponent from 'react-syntax-highlighter'
import { githubGist } from 'react-syntax-highlighter/dist/esm/styles/hljs'
import './SyntaxHighlighter.less'

export interface SyntaxHighlighterProps {
  children: string
  firstLineNumber?: number
  highlightLines?: number[]
}

const SyntaxHighlighter: React.FC<SyntaxHighlighterProps> = props => {
  const { highlightLines, firstLineNumber } = props

  let lineHighlighter: lineTagPropsFunction | undefined
  if (highlightLines && highlightLines.length) {
    lineHighlighter = lineNumber => {
      // lineNumber is always starting at 1 regardless of startingLineNumber
      let actualLineNumber = lineNumber
      if (firstLineNumber) {
        actualLineNumber += firstLineNumber - 1
      }
      if (highlightLines.indexOf(actualLineNumber) !== -1) {
        return {
          className: 'highlight-line'
        }
      }
      return {}
    }
  }

  return (
    <SyntaxHighlighterComponent
      className="syntax-highlighter"
      lineProps={lineHighlighter}
      wrapLines={lineHighlighter !== undefined}
      showLineNumbers
      startingLineNumber={props.firstLineNumber || 1}
      style={githubGist}
    >
      {props.children}
    </SyntaxHighlighterComponent>
  )
}

export default SyntaxHighlighter
