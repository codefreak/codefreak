import React, { useState } from 'react'
import { Button, Col, Form, Input, Modal, Popconfirm, Row } from 'antd'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import { ButtonProps } from 'antd/lib/button'

interface EditStringArrayModalProps {
  initialValues: string[]
  onSave: (newValues: string[]) => unknown
  onCancel?: () => unknown
  visible: boolean
  extraContent?: React.ReactNode
  title: string
}

const formItemLayout = {
  labelCol: {
    xs: { span: 24 },
    sm: { span: 4 }
  },
  wrapperCol: {
    xs: { span: 24 },
    sm: { span: 20 }
  }
}
const formItemLayoutWithOutLabel = {
  wrapperCol: {
    xs: { span: 24, offset: 0 },
    sm: { span: 20, offset: 4 }
  }
}

const EditStringArrayModal: React.FC<EditStringArrayModalProps> = props => {
  const { initialValues, onSave, onCancel, visible, extraContent, title } =
    props
  const [form] = Form.useForm()
  return (
    <Modal
      width={420}
      visible={visible}
      title={title}
      footer={
        <Row>
          <Col span="12" style={{ textAlign: 'left' }}>
            <Popconfirm
              title="Really undo changes?"
              onConfirm={() => {
                form.resetFields()
              }}
              placement="left"
            >
              <Button danger>Reset</Button>
            </Popconfirm>
          </Col>
          <Col span="12">
            <Button
              onClick={() => {
                onCancel?.()
              }}
            >
              Cancel
            </Button>
            <Button
              type="primary"
              onClick={async () => {
                try {
                  const values = await form.validateFields()
                  onSave(values.entries)
                } catch (e) {
                  // validation failed
                }
              }}
            >
              Save
            </Button>
          </Col>
        </Row>
      }
    >
      {extraContent}
      <Form
        form={form}
        name="evaluation-step-definition"
        initialValues={{
          entries: initialValues || []
        }}
        labelCol={{ span: 6 }}
        wrapperCol={{ span: 18 }}
      >
        <Form.List name="entries">
          {(fields, { add, remove }, { errors }) => (
            <>
              {fields.map((field, index) => (
                <Form.Item
                  {...(index === 0
                    ? formItemLayout
                    : formItemLayoutWithOutLabel)}
                  label={index === 0 ? 'Patterns' : ''}
                  required={false}
                  key={field.key}
                >
                  <Form.Item
                    {...field}
                    validateTrigger={['onChange', 'onBlur']}
                    rules={[
                      {
                        required: true,
                        whitespace: true,
                        message:
                          'Please enter a valid pattern or remove this entry.'
                      }
                    ]}
                    noStyle
                  >
                    <Input
                      placeholder="pattern"
                      autoFocus
                      style={{ width: '90%' }}
                    />
                  </Form.Item>
                  <span style={{ width: '10%' }}>
                    <MinusCircleOutlined
                      className="dynamic-delete-button"
                      onClick={() => remove(field.name)}
                      style={{ marginLeft: 5 }}
                    />
                  </span>
                </Form.Item>
              ))}
              <Form.Item {...formItemLayoutWithOutLabel}>
                <Button
                  type="dashed"
                  onClick={() => add()}
                  style={{ width: '90%' }}
                  icon={<PlusOutlined />}
                >
                  Add pattern
                </Button>
                <Form.ErrorList errors={errors} />
              </Form.Item>
            </>
          )}
        </Form.List>
      </Form>
    </Modal>
  )
}

export type EditStringArrayButtonProps = Omit<
  EditStringArrayModalProps,
  'visible'
> &
  ButtonProps

export const EditStringArrayButton: React.FC<EditStringArrayButtonProps> =
  props => {
    const {
      onCancel: onRealCancel,
      onSave: onRealSave,
      initialValues,
      extraContent,
      title,
      children,
      ...buttonProps
    } = props
    const [visible, setVisible] = useState(false)

    const onSave: EditStringArrayModalProps['onSave'] = async values => {
      await onRealSave(values)
      setVisible(false)
    }

    const onCancel: EditStringArrayModalProps['onCancel'] = async () => {
      await onRealCancel?.()
      setVisible(false)
    }

    return (
      <>
        <EditStringArrayModal
          initialValues={initialValues}
          onSave={onSave}
          onCancel={onCancel}
          visible={visible}
          title={title}
        />
        <Button {...buttonProps} onClick={() => setVisible(true)}>
          {children}
        </Button>
      </>
    )
  }

export default EditStringArrayModal
