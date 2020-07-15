import { Icon, Tag, Tooltip } from 'antd'
import { TagProps } from 'antd/es/tag'
import { TooltipProps } from 'antd/es/tooltip'
import { Moment } from 'moment'
import React, { HTMLProps, useEffect, useState } from 'react'
import { secondsToRelTime } from '../../services/time'
import Countdown from '../Countdown'
import TimeLimitEditModal from './TimeLimitEditModal'
import './TimeLimitTag.less'

interface TimeLimitTagProps extends HTMLProps<HTMLSpanElement> {
  timeLimit: number
  deadline?: Moment
  suffix?: React.ReactNode
}

const TimeLimitTag: React.FC<TimeLimitTagProps> = ({
  timeLimit,
  deadline,
  suffix,
  ...htmlProps
}) => {
  const [isOver, setIsOver] = useState<boolean>()

  // handle deadline changes properly
  useEffect(() => {
    setIsOver(!!deadline && deadline.isSameOrBefore())
  }, [deadline, setIsOver])

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
        <Icon type="clock-circle" /> {tagProps.children} {suffix}
      </Tag>
    </Tooltip>
  )
}

interface EditableTimeLimitTagProps
  extends Omit<TimeLimitTagProps, 'timeLimit' | 'onChange'> {
  taskId: string
  timeLimit?: number
  onChange?: (seconds: number | undefined) => void
}

export const EditableTimeLimitTag: React.FC<EditableTimeLimitTagProps> = ({
  taskId,
  onChange,
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
      <TimeLimitTag
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
    if (onChange) {
      onChange(value)
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

export default TimeLimitTag
