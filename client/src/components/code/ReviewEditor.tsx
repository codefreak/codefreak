import { Spin } from 'antd'
import React, { useState } from 'react'
import { sliceLines } from '../../services/file'
import ReviewCommentForm, { ReviewCommentValues } from './ReviewComment'
import './ReviewEditor.less'
import SyntaxHighlighter, { SyntaxHighlighterProps } from './SyntaxHighlighter'

export interface ReviewEditorProps {
  syntaxHighlighterProps?: SyntaxHighlighterProps
  children: string
}

const ReviewEditor: React.FC<ReviewEditorProps> = props => {
  const { children } = props
  const [currentLineNumber, setCurrentLineNumber] = useState<
    number | undefined
  >()

  const onLineNumberClick = (lineNumber: number, element: HTMLSpanElement) => {
    setCurrentLineNumber(lineNumber)
  }

  const highlighterProps = {
    onLineNumberClick
  }

  // simply return the highlighted code if we are not reviewing a line currently
  if (currentLineNumber === undefined) {
    return (
      <div className="review-editor">
        <SyntaxHighlighter {...highlighterProps}>{children}</SyntaxHighlighter>
      </div>
    )
  }

  const onClose = () => {
    setCurrentLineNumber(undefined)
  }

  const onComment = (values: ReviewCommentValues) => {
    setCurrentLineNumber(undefined)
  }

  // split the syntax highlighter into two parts if we are reviewing lines
  return (
    <div className="review-editor">
      <SyntaxHighlighter {...highlighterProps}>
        {sliceLines(children, 1, currentLineNumber) + '\n'}
      </SyntaxHighlighter>
      <Spin spinning={false} tip="Creating comment…">
        <ReviewCommentForm
          onSubmit={onComment}
          onClose={onClose}
          title={`Create a comment on line ${currentLineNumber}`}
        />
      </Spin>
      <SyntaxHighlighter
        {...highlighterProps}
        firstLineNumber={currentLineNumber + 1}
      >
        {sliceLines(children, currentLineNumber + 1)}
      </SyntaxHighlighter>
    </div>
  )
}

export default ReviewEditor
