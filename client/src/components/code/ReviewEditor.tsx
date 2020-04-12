import { Spin } from 'antd'
import React, { useState } from 'react'
import {
  FeedbackSeverity,
  useCreateCommentFeedbackMutation
} from '../../generated/graphql'
import { sliceLines } from '../../services/file'
import { messageService } from '../../services/message'
import ReviewCommentForm, { ReviewCommentValues } from './ReviewComment'
import './ReviewEditor.less'
import SyntaxHighlighter, { SyntaxHighlighterProps } from './SyntaxHighlighter'
import {basename} from "path"

export interface ReviewEditorProps {
  syntaxHighlighterProps?: SyntaxHighlighterProps
  children: string
  answerId: string
  path: string
  fileCollectionDigest: string
}

const ReviewEditor: React.FC<ReviewEditorProps> = props => {
  const { children } = props
  const [currentLineNumber, setCurrentLineNumber] = useState<
    number | undefined
  >()
  const [createComment, { loading }] = useCreateCommentFeedbackMutation()

  const highlighterProps = {
    onLineNumberClick: setCurrentLineNumber
  }

  // simply return the highlighted code if we are not reviewing a line currently
  if (currentLineNumber === undefined) {
    return (
      <div className="review-editor">
        <SyntaxHighlighter {...highlighterProps}>{children}</SyntaxHighlighter>
      </div>
    )
  }

  const onClose = () => setCurrentLineNumber(undefined)

  const onComment = (values: ReviewCommentValues) => {
    createComment({
      variables: {
        answerId: props.answerId,
        comment: values.comment,
        digest: props.fileCollectionDigest,
        severity: values.severity || FeedbackSeverity.Info,
        path: props.path,
        line: currentLineNumber
      }
    }).then(() => {
      messageService.success(
        `Comment has been added to ${basename(
          props.path
        )} line ${currentLineNumber}!`
      )
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
