import { Moment } from 'moment'
import React from 'react'
import ReactCountdown, { CountdownRenderProps } from 'react-countdown'
import { toRelTime } from '../services/time'

const countdownRenderer = (props: CountdownRenderProps): React.ReactNode => {
  if (props.completed) {
    return toRelTime(0, 0, 0)
  }

  const { days, hours, minutes, seconds } = props
  return toRelTime(days * 24 + hours, minutes, seconds, true)
}

interface CountdownProps {
  date: Moment
  onComplete?: () => void
}

const Countdown: React.FC<CountdownProps> = ({ date, onComplete }) => {
  return (
    <ReactCountdown
      date={date.toDate()}
      renderer={countdownRenderer}
      onComplete={onComplete}
    />
  )
}

export default Countdown
