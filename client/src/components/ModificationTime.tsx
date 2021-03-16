import moment from 'moment'
import { HTMLProps } from 'react'
import { Tooltip } from 'antd'
import { useFormatter } from '../hooks/useFormatter'
import { useServerMoment } from '../hooks/useServerTimeOffset'

interface ModificationTimeProps extends HTMLProps<HTMLTimeElement> {
  created: Date
  updated: Date
}

const ModificationTime = (props: ModificationTimeProps) => {
  const { created, updated, ...timeProps } = props
  const { dateTime } = useFormatter()
  const createdDateMoment = moment(props.created)
  const updatedDateMoment = moment(props.updated)
  const serverMoment = useServerMoment()
  const updatedFormattedAbsoluteDate = dateTime(updated)
  const createdFormattedRelativeDate = createdDateMoment.from(serverMoment())

  // show relative createdAt date only in the tooltip and absolute updatedAt date directly
  return (
    <time dateTime={updatedDateMoment.toISOString()} {...timeProps}>
      <Tooltip title={`Created ${createdFormattedRelativeDate}`}>
        {updatedFormattedAbsoluteDate}
      </Tooltip>
    </time>
  )
}

export default ModificationTime
