import { Spin } from 'antd'
import React, { useState } from 'react'
import {
  FeedbackSeverity,
  useCreateCommentMutation
} from '../../generated/graphql'
import { sliceLines } from '../../services/file'
import ReviewCommentForm, { ReviewCommentValues } from './ReviewComment'
import './ReviewEditor.less'
import SyntaxHighlighter, { SyntaxHighlighterProps } from './SyntaxHighlighter'

export interface ReviewEditorProps {
  syntaxHighlighterProps?: SyntaxHighlighterProps
  children: string
  answerId: string
  path: string
  fileCollectionDigist: string
}

const ReviewEditor: React.FC<ReviewEditorProps> = props => {
  const { children } = props
  const [currentLineNumber, setCurrentLineNumber] = useState<
    number | undefined
  >()
  const [createComment, { data, loading, error }] = useCreateCommentMutation()

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
    createComment({
      variables: {
        answerId: props.answerId,
        comment: values.comment,
        digest: props.fileCollectionDigist,
        severity: values.severity || FeedbackSeverity.Info,
        path: props.path
      }
    }).then(() => {
      setCurrentLineNumber(undefined)
    })
  }

  // split the syntax highlighter into two parts if we are reviewing lines
  return (
    <div className="review-editor">
      <SyntaxHighlighter {...highlighterProps}>
        {sliceLines(children, 1, currentLineNumber) + '\n'}
      </SyntaxHighlighter>
      <Spin spinning={loading} tip="Creating comment…">
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
