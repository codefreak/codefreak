import React from 'react'
import SyntaxHighlighterComponent from 'react-syntax-highlighter'
import 'highlight.js/styles/github-gist.css'
import './SyntaxHighlighter.less'

export interface SyntaxHighlighterProps {
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
      lineProps={lineHighlighter}
      wrapLines={lineHighlighter !== undefined}
      showLineNumbers
      useInlineStyles={false}
      startingLineNumber={props.firstLineNumber || 1}
      lineNumberContainerProps={{ className: 'hljs-line-numbers', style: {} }}
    >
      {props.children}
    </SyntaxHighlighterComponent>
  )
}

export default SyntaxHighlighter
