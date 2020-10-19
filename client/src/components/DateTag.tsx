import moment from 'moment'
import React from 'react'
import { Icon, Tag, Tooltip } from 'antd'

export enum DateType {
  CREATED,
  UPDATED
}

type DateTagInfo = {
  title: string
  color: string
  iconType: string
}

const DateTagMapping: {
  [key in DateType]: DateTagInfo
} = {
  [DateType.CREATED]: {
    title: 'Created',
    color: 'blue',
    iconType: 'file-add'
  },
  [DateType.UPDATED]: {
    title: 'Updated',
    color: 'orange',
    iconType: 'save'
  }
}

type DateTagProps = {
  dateType: DateType
  contentType: string
  date: Date
}

const DateTag = (props: DateTagProps) => {
  const info = DateTagMapping[props.dateType]
  const formattedDate = moment(props.date).fromNow()
  const description = `The ${props.contentType.toLocaleLowerCase()} was ${info.title.toLowerCase()} ${formattedDate}`

  return (
    <Tooltip title={description}>
      <Tag color={info.color}>
        <Icon type={info.iconType} /> {info.title} {formattedDate}
      </Tag>
    </Tooltip>
  )
}

export default DateTag
