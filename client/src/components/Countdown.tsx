import { Moment } from 'moment'
import React from 'react'
import ReactCountdown, {
  CountdownProps as ReactCountdownProps,
  CountdownRenderProps
} from 'react-countdown'
import { useServerNow } from '../hooks/useServerTimeOffset'
import { componentsToRelTime } from '../services/time'

const defaultCountdownRenderer = (
  props: CountdownRenderProps
): React.ReactNode => {
  if (props.completed && !props.props.overtime) {
    return componentsToRelTime({ hours: 0, minutes: 0, seconds: 0 })
  }

  return componentsToRelTime(props, true)
}

interface CountdownProps {
  date: Moment
  onComplete?: ReactCountdownProps['onComplete']
  overTime?: boolean
}

const Countdown: React.FC<CountdownProps> = props => {
  const { date, onComplete, overTime } = props
  const serverNow = useServerNow()

  return (
    <ReactCountdown
      date={date.toDate()}
      renderer={defaultCountdownRenderer}
      onComplete={onComplete}
      now={serverNow}
      overtime={overTime}
    />
  )
}

export default Countdown
