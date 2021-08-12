import React, { useEffect } from 'react'
import {
  EvaluationStepDefinition,
  EvaluationStepDefinitionInput,
  useGetSupportedEvaluationReportFormatsQuery
} from '../generated/graphql'
import { Checkbox, Form, FormInstance, Input, Modal, Select } from 'antd'
import { TimeIntervalSecInput } from './TimeIntervalInput'
import './EditEvaluationStepDefinitionModal.less'

export interface EditEvaluationStepDefinitionModalProps {
  initialValues: EvaluationStepDefinition
  onSave: (newSettings: Partial<EvaluationStepDefinitionInput>) => unknown
  onCancel: () => unknown
  visible: boolean
}

const EvaluationStepDefinitionForm = (props: {
  initialValues: EvaluationStepDefinition
  form?: FormInstance
}) => {
  const evaluationReportFormats =
    useGetSupportedEvaluationReportFormatsQuery()?.data
      ?.evaluationReportFormats || []

  useEffect(() => {
    // update form values if initial values change
    if (props.form) {
      props.form.setFieldsValue(props.initialValues)
    }
  }, [props.form, props.initialValues])

  return (
    <Form
      form={props.form}
      name="evaluation-step-definition"
      initialValues={props.initialValues}
      labelCol={{ span: 6 }}
      wrapperCol={{ span: 18 }}
    >
      <Form.Item label="Active" name="active" valuePropName="checked">
        <Checkbox />
      </Form.Item>
      <Form.Item label="Title" name="title" rules={[{ required: true }]}>
        <Input />
      </Form.Item>
      <Form.Item
        label="Report Format"
        name="reportFormat"
        rules={[{ required: true }]}
      >
        <Select loading={evaluationReportFormats.length === 0}>
          {evaluationReportFormats.map(format => (
            <Select.Option key={format.key} value={format.key}>
              {format.title} (<code>{format.key}</code>)
            </Select.Option>
          ))}
        </Select>
      </Form.Item>
      <Form.Item label="Report Path Pattern" name="reportPath">
        <Input />
      </Form.Item>
      <Form.Item label="Custom Timeout" name="timeout">
        <TimeIntervalSecInput nullable />
      </Form.Item>
      <Form.Item
        name="script"
        labelCol={{ span: 24 }}
        wrapperCol={{ span: 24 }}
        label="Evaluation Script"
      >
        <Input.TextArea
          rows={12}
          className="evaluation-step-definition-modal-script"
        />
      </Form.Item>
    </Form>
  )
}

const EditEvaluationStepDefinitionModal: React.FC<EditEvaluationStepDefinitionModalProps> =
  props => {
    const { initialValues, onSave, onCancel, visible } = props
    const [form] = Form.useForm()

    return (
      <Modal
        width={620}
        visible={visible}
        title={`Edit evaluation step "${initialValues.title}"`}
        okText="Save"
        onOk={async () => {
          try {
            const values = await form.validateFields()
            onSave(values)
          } catch (e) {
            // validation failed
          }
        }}
        onCancel={() => onCancel()}
      >
        <EvaluationStepDefinitionForm
          initialValues={initialValues}
          form={form}
        />
      </Modal>
    )
  }

export default EditEvaluationStepDefinitionModal
