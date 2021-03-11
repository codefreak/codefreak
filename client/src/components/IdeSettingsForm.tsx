import React, { ChangeEventHandler, useState } from 'react'
import { Button, Col, Form, Input, Row, Select } from 'antd'
import CodefreakDocsLink from './CodefreakDocsLink'
import useSystemConfig from '../hooks/useSystemConfig'
import idePresets, { IdePreset } from '../data/ide-presets.yaml'

export interface IdeSettingsModel {
  ideImage?: string
  ideArguments?: string
}

export interface IdeSettingsFormProps {
  defaultValue?: IdeSettingsModel
  onChange?: (newValues: IdeSettingsModel) => void
}

const renderPresetOptions = (
  prefix: string,
  group: Record<string, IdePreset>
) => {
  return Object.keys(group).map(presetKey => {
    const { title } = group[presetKey]
    const key = `${prefix}.${presetKey}`
    return (
      <Select.Option key={key} value={key}>
        {title}
      </Select.Option>
    )
  })
}

const renderPresetOptionGroups = () => {
  return Object.keys(idePresets).map(groupKey => {
    const { title, items } = idePresets[groupKey] as {
      title?: string
      items: Record<string, IdePreset>
    }
    return (
      <Select.OptGroup key={groupKey} label={title}>
        {renderPresetOptions(groupKey, items)}
      </Select.OptGroup>
    )
  })
}

const IdeSettingsForm: React.FC<IdeSettingsFormProps> = props => {
  const { defaultValue, onChange } = props
  const { data: defaultIdeImage } = useSystemConfig('defaultIdeImage')
  const [values, setValues] = useState<IdeSettingsModel>(
    defaultValue || {
      ideImage: undefined,
      ideArguments: undefined
    }
  )

  const setValue = <K extends keyof IdeSettingsModel>(
    field: K
  ): ChangeEventHandler<HTMLInputElement> => e => {
    const newValues = {
      ...values,
      [field]: e.target.value
    }
    setValues(newValues)
    onChange?.(newValues)
  }

  const onPresetApply = (formValues: { preset: string }) => {
    const [groupKey, presetKey] = formValues.preset.split('.')
    const group = idePresets[groupKey]
    if (group.items.hasOwnProperty(presetKey)) {
      const { title, ...newValues } = group.items[presetKey]
      setValues(newValues)
      onChange?.(newValues)
    }
  }

  return (
    <Row gutter={16}>
      <Col span={8}>
        <h3>IDE Presets</h3>
        <p>
          You can select a preset from the list below which will fill your
          "Image" and "CMD" fields with some sensible presets for the selected
          purpose. Language-specific presets are meant to be used with their
          corresponding Task templates (e.g. Java expects its main file at{' '}
          <code>src/main/java/Main.java</code>).
        </p>
        <Form onFinish={onPresetApply}>
          <Form.Item name="preset">
            <Select defaultActiveFirstOption style={{ width: 400 }}>
              {renderPresetOptionGroups()}
            </Select>
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit">
              Apply IDE preset
            </Button>
          </Form.Item>
        </Form>
      </Col>
      <Col span={8}>
        <h3>Image</h3>
        <p>
          Optionally, you can specify a custom Docker image for the student
          Online IDE. You will most likely <em>not</em> need this! Read more
          about custom IDE images{' '}
          <CodefreakDocsLink category="for-teachers" page="ide">
            here
          </CodefreakDocsLink>
          .
        </p>
        <p>
          Leave blank to use the default image <code>{defaultIdeImage}</code>.
        </p>
        <Input
          style={{
            maxWidth: 400
          }}
          value={values.ideImage}
          placeholder="e.g. foo/bar:latest"
          allowClear
          onChange={setValue('ideImage')}
        />
      </Col>
      <Col span={8}>
        <h3>CMD / Arguments</h3>
        <p>
          You can customize the CMD on the container to alter either the
          container CMD or pass additional arguments to the container. Only use
          this if you know what you are doing.
          <br />
          <strong>Warning:</strong> These values are NOT parameters for{' '}
          <code>docker run</code>!
        </p>
        <Input
          style={{
            maxWidth: 400
          }}
          value={values.ideArguments}
          placeholder="--option=value --verbose"
          allowClear
          onChange={setValue('ideArguments')}
        />
      </Col>
    </Row>
  )
}

export default IdeSettingsForm
