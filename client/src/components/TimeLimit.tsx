import { Checkbox, Form, Icon, InputNumber, Modal, Tag, Tooltip } from 'antd'
import { Moment } from 'moment'
import React, { HTMLProps, useState } from 'react'

import { CheckboxChangeEvent } from 'antd/es/checkbox'
import { TagProps } from 'antd/es/tag'
import { TooltipProps } from 'antd/es/tooltip'
import {
  componentsToSeconds,
  secondsToComponents,
  secondsToRelTime,
  TimeComponents
} from '../services/time'
import Countdown from './Countdown'
import './TimeLimit.less'

interface TimeLimitProps extends HTMLProps<HTMLSpanElement> {
  timeLimit: number
  deadline?: Moment
  suffix?: React.ReactNode
}

const TimeLimit: React.FC<TimeLimitProps> = ({
  timeLimit,
  deadline,
  suffix,
  ...htmlProps
}) => {
  const [isOver, setIsOver] = useState<boolean>(
    !!deadline && deadline.isSameOrBefore()
  )

  const relTime = secondsToRelTime(timeLimit)
  const tooltipProps: TooltipProps = {
    title: `There is a ${relTime} time limit on this task.`
  }
  const tagProps: TagProps = {
    ...htmlProps,
    className: `time-limit ${htmlProps.className}`,
    color: 'blue',
    children: relTime
  }

  if (deadline !== undefined) {
    if (isOver) {
      tooltipProps.title = 'Time is up. You cannot change your answer anymore.'
      tagProps.color = 'magenta'
      tagProps.children = "Time's up"
    } else {
      const onComplete = () => setIsOver(true)
      const countdown = <Countdown date={deadline!!} onComplete={onComplete} />
      tooltipProps.title = <>You have {countdown} left to finish this task.</>
      tooltipProps.className += ' running'
      tagProps.color = 'orange'
      tagProps.children = countdown
    }
  }

  return (
    <Tooltip {...tooltipProps}>
      <Tag {...tagProps}>
        {tagProps.children} {suffix}
      </Tag>
    </Tooltip>
  )
}

const renderTimeIntervalInput = (
  suffix: string,
  value: number,
  onChange: (value: number) => void
) => {
  const onChangeDefinitely = (val: number | undefined) =>
    val !== undefined ? onChange(val) : undefined

  const parser = (val: string | undefined) => {
    if (!val) {
      return ''
    }
    return val.replace(/[^\d]+/, '')
  }
  const formatter = (val: string | number | undefined) => `${val}${suffix}`

  return (
    <InputNumber
      min={0}
      onChange={onChangeDefinitely}
      value={value}
      parser={parser}
      formatter={formatter}
    />
  )
}

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
  const originalComponents = secondsToComponents(timeLimit)
  const { hours, minutes, seconds } = originalComponents

  const createOnValueChange = (field: keyof TimeComponents) => (
    value: number | undefined
  ) => {
    if (onChangeValue) {
      const newTimeLimit = componentsToSeconds({
        ...originalComponents,
        [field]: value
      })
      onChangeValue(newTimeLimit)
    }
  }

  const onCheckboxChange = (state: CheckboxChangeEvent) => {
    if (onChangeStatus) {
      onChangeStatus(state.target.checked)
    }
  }

  return (
    <Form layout="vertical">
      <Form.Item>
        <Checkbox checked={enabled} onChange={onCheckboxChange}>
          Enable time limit
        </Checkbox>
      </Form.Item>
      {enabled && (
        <Form.Item label="New time limit">
          {renderTimeIntervalInput('h', hours, createOnValueChange('hours'))}
          {renderTimeIntervalInput(
            'm',
            minutes,
            createOnValueChange('minutes')
          )}
          {renderTimeIntervalInput(
            's',
            seconds,
            createOnValueChange('seconds')
          )}
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
        this task they will have the following amount of time to finish the
        task.
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

interface EditableTimeLimitProps extends Omit<TimeLimitProps, 'timeLimit'> {
  taskId: string
  timeLimit?: number
  onTimeLimitChange?: (seconds: number | undefined) => void
}

export const EditableTimeLimit: React.FC<EditableTimeLimitProps> = ({
  taskId,
  onTimeLimitChange,
  ...props
}) => {
  const [isEditing, setIsEditing] = useState<boolean>(false)
  const [timeLimit, setTimeLimit] = useState(props.timeLimit)
  const onClickEdit = () => {
    setIsEditing(true)
  }

  let timeLimitNode: React.ReactNode
  if (timeLimit) {
    timeLimitNode = (
      <TimeLimit
        {...props}
        className="editable"
        timeLimit={timeLimit}
        suffix={<Icon type="edit" />}
        onClick={onClickEdit}
      />
    )
  } else {
    timeLimitNode = (
      <Tag onClick={onClickEdit} className="time-limit editable">
        No time limit <Icon type="edit" />
      </Tag>
    )
  }

  const onCancel = () => setIsEditing(false)
  const onOk = (value: number | undefined) => {
    setTimeLimit(value)
    setIsEditing(false)
    if (onTimeLimitChange) {
      onTimeLimitChange(value)
    }
  }

  return (
    <>
      <TimeLimitEditModal
        initialValue={timeLimit}
        onOk={onOk}
        visible={isEditing}
        onCancel={onCancel}
      />
      {timeLimitNode}
    </>
  )
}

export default TimeLimit
