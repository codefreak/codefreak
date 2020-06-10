import { Tag, Tooltip } from 'antd'
import { Moment } from 'moment'
import React, { useState } from 'react'

import { secondsToRelTime } from '../services/time'
import Countdown from './Countdown'
import './TimeLimit.less'

interface TimeLimitProps {
  timeLimit: number
  startedAt?: Moment
}

const TimeLimit: React.FC<TimeLimitProps> = ({ timeLimit, startedAt }) => {
  const endTime = startedAt ? startedAt.add(timeLimit, 'seconds') : undefined
  const [isOver, setIsOver] = useState<boolean>(
    !!endTime && endTime.isSameOrBefore()
  )

  if (startedAt !== undefined) {
    if (isOver) {
      return (
        <Tooltip title="Time is up. You cannot change your answer anymore.">
          <Tag className="time-limit" color="magenta">
            Time's up
          </Tag>
        </Tooltip>
      )
    }
    const onComplete = () => setIsOver(true)
    const countdown = <Countdown date={endTime!!} onComplete={onComplete} />
    return (
      <Tooltip title={<>You have {countdown} left to finish this task.</>}>
        <Tag className="time-limit running" color="orange">
          {countdown}
        </Tag>
      </Tooltip>
    )
  }
  const relTime = secondsToRelTime(timeLimit)
  return (
    <Tooltip
      title={
        <>
          There is a {relTime} time limit on this task. You have{' '}
          <strong>not</strong> started, yet.
        </>
      }
    >
      <Tag className="time-limit" color="blue">
        {relTime}
      </Tag>
    </Tooltip>
  )
}

export default TimeLimit
