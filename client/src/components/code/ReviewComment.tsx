import { Button, Card, Col, Form, Icon, Popconfirm, Row, Select } from 'antd'
import { FormComponentProps } from 'antd/es/form'
import TextArea from 'antd/es/input/TextArea'
import React, { FormEvent, useState } from 'react'
import { FeedbackSeverity } from '../../generated/graphql'
import useAuthenticatedUser from '../../hooks/useAuthenticatedUser'
import Avatar from '../user/Avatar'

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

export interface ReviewCommentValues {
  comment: string
  severity?: FeedbackSeverity
}

export interface ReviewCommentFormProps extends FormComponentProps {
  title?: string
  onSubmit?: (values: ReviewCommentValues) => void
  onClose?: () => void
}

const ReviewCommentForm: React.FC<ReviewCommentFormProps> = props => {
  const { getFieldDecorator } = props.form
  const [hasValue, setHasValue] = useState<boolean>(false)
  const user = useAuthenticatedUser()

  const onSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    props.form.validateFieldsAndScroll((err, values) => {
      if (!err && props.onSubmit) {
        props.onSubmit(values)
      }
    })
  }

  const onClose = () => {
    if (props.onClose) {
      props.onClose()
    }
  }

  // confirm close if comment contains a value
  const onChange = () => setHasValue(!!props.form.getFieldValue('comment'))
  let close = <Icon type="close" onClick={!hasValue ? onClose : undefined} />
  if (hasValue) {
    close = (
      <Popconfirm title="Discard comment?" onConfirm={onClose}>
        {close}
      </Popconfirm>
    )
  }

  return (
    <Card
      size="small"
      className="review-comment"
      title={props.title}
      extra={close}
    >
      <Row type="flex">
        <Col>
          <Avatar user={user} />
        </Col>
        <Col style={{ flexGrow: 1 }}>
          <Form onSubmit={onSubmit} onChange={onChange}>
            <Form.Item hasFeedback>
              {getFieldDecorator('comment', {
                rules: [
                  {
                    required: true,
                    message: 'Please enter a comment'
                  }
                ]
              })(
                <TextArea
                  autoSize={{ minRows: 3, maxRows: 6 }}
                  placeholder={`Add a useful comment…`}
                />
              )}
            </Form.Item>
            <Row>
              <Col span={12}>
                <Form.Item hasFeedback>
                  {getFieldDecorator('severity')(renderSeveritySelect())}
                </Form.Item>
              </Col>
              <Col span={12} style={{ textAlign: 'right' }}>
                <Button type="primary" htmlType="submit">
                  Save Comment
                </Button>
              </Col>
            </Row>
          </Form>
        </Col>
      </Row>
    </Card>
  )
}

export default Form.create<ReviewCommentFormProps>({ name: 'review-comment' })(
  ReviewCommentForm
)
