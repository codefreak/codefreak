import { Button, Card, Col, Form, Icon, Row, Select } from 'antd'
import TextArea from 'antd/es/input/TextArea'
import React, { useState } from 'react'
import { FeedbackSeverity } from '../../services/codefreak-api'
import { sliceLines } from '../../services/file'
import './ReviewEditor.less'
import SyntaxHighlighter, { SyntaxHighlighterProps } from './SyntaxHighlighter'

const renderSeveritySelect = () => {
  return (
    <Select
      style={{ width: '250px' }}
      placeholder="Select a severity"
      allowClear
    >
      {Object.entries(FeedbackSeverity).map(([value, key]) => {
        return (
          <Select.Option key={key} value={key}>
            {value}
          </Select.Option>
        )
      })}
    </Select>
  )
}

const CommentForm: React.FC = () => {
  return (
    <Form>
      <TextArea
        autoSize={{ minRows: 3, maxRows: 6 }}
        placeholder={`Add a useful comment…`}
      />
      <Row>
        <Col span={12}>{renderSeveritySelect()}</Col>
        <Col span={12} style={{ textAlign: 'right' }}>
          <Button type="primary">Save Comment</Button>
        </Col>
      </Row>
    </Form>
  )
}

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

  // split the syntax highlighter into two parts if we are reviewing lines
  return (
    <div className="review-editor">
      <SyntaxHighlighter {...highlighterProps}>
        {sliceLines(children, 1, currentLineNumber) + '\n'}
      </SyntaxHighlighter>
      <Card
        size="small"
        className="review-comment"
        title={`Comment on line ${currentLineNumber}`}
        extra={<Icon type="close" onClick={onClose} />}
      >
        <CommentForm />
      </Card>
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
