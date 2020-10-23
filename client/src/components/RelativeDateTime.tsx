import moment from 'moment'
import React, { HTMLProps } from 'react'
import { Tooltip } from 'antd'
import { useFormatter } from '../hooks/useFormatter'
import { useServerMoment } from '../hooks/useServerTimeOffset'

interface RelativeDateTimeProps extends HTMLProps<HTMLTimeElement> {
  date: Date
}

const RelativeDateTime = (props: RelativeDateTimeProps) => {
  const { date, ...timeProps } = props
  const { dateTime } = useFormatter()
  const dateMoment = moment(props.date)
  const serverMoment = useServerMoment()
  const formattedAbsoluteDate = dateTime(date)
  const formattedRelativeDate = dateMoment.from(serverMoment())

  return (
    <time dateTime={dateMoment.toISOString()} {...timeProps}>
      <Tooltip title={formattedAbsoluteDate}>{formattedRelativeDate}</Tooltip>
    </time>
  )
}

export default RelativeDateTime
