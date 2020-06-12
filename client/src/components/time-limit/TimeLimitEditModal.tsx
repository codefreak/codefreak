import { Checkbox, Form, Modal } from 'antd'
import { CheckboxChangeEvent } from 'antd/es/checkbox'
import React, { useState } from 'react'
import {
  componentsToSeconds,
  secondsToComponents,
  TimeComponents
} from '../../services/time'
import TimeIntervalInput from '../TimeIntervalInput'

interface TimeLimitFormProps {
  enabled: boolean
  timeLimit: number
  onChangeStatus?: (state: boolean) => void
  onChangeValue?: (timeLimit: number) => void
}

const TimeLimitForm: React.FC<TimeLimitFormProps> = ({
  onChangeValue,
  onChangeStatus,
  enabled,
  timeLimit
}) => {
  const onCheckboxChange = (state: CheckboxChangeEvent) => {
    if (onChangeStatus) {
      onChangeStatus(state.target.checked)
    }
  }

  const onIntervalChange = (components: TimeComponents) => {
    if (onChangeValue) {
      onChangeValue(componentsToSeconds(components))
    }
  }

  return (
    <Form
      labelCol={{
        xs: { span: 8 }
      }}
      wrapperCol={{
        xs: { span: 16 }
      }}
    >
      <Form.Item label="Enable time limit">
        <Checkbox checked={enabled} onChange={onCheckboxChange} />
      </Form.Item>
      {enabled && (
        <Form.Item label="New time limit">
          <TimeIntervalInput
            onChange={onIntervalChange}
            defaultValue={secondsToComponents(timeLimit)}
          />
        </Form.Item>
      )}
    </Form>
  )
}

const TimeLimitEditModal: React.FC<{
  initialValue: number | undefined
  onCancel: () => void
  onOk: (value: number | undefined) => void
  visible: boolean
}> = ({ initialValue, onOk, onCancel, visible }) => {
  const [isEnabled, setIsEnabled] = useState<boolean>(!!initialValue)
  const [timeLimit, setTimeLimit] = useState<number | undefined>(initialValue)

  const onModalOk = () => {
    // if time limit is set and greater than 0
    onOk(isEnabled && timeLimit ? timeLimit : undefined)
  }

  return (
    <Modal
      title="Change time limit for this task"
      visible={visible}
      onCancel={onCancel}
      onOk={onModalOk}
    >
      <p>
        You can set a time limit for this task. After students start working on
        this task they will have the configured amount of time to finish the
        task. After the time limit has been reached students cannot modify their
        answers anymore.
      </p>
      <p>
        Editing an answer is never possible after the assignment deadline has
        been reached.
      </p>
      <TimeLimitForm
        enabled={isEnabled}
        timeLimit={timeLimit || 0}
        onChangeValue={setTimeLimit}
        onChangeStatus={setIsEnabled}
      />
    </Modal>
  )
}

export default TimeLimitEditModal
