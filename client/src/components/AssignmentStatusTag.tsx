import { Icon, Tag, Tooltip } from 'antd'
import React from 'react'
import { AssignmentStatus } from '../generated/graphql'

interface AssignmentStatusTagProps {
  status: AssignmentStatus
}

interface AssignmentStatusInfo {
  title: string
  description: string
  color?: string
  iconType?: string
}

const AssignmentStatusInfoMapping: {
  [key in AssignmentStatus]: AssignmentStatusInfo
} = {
  [AssignmentStatus.Inactive]: {
    title: 'Inactive',
    description: 'This assignment is only visible for you.',
    iconType: 'eye-invisible'
  },
  [AssignmentStatus.Active]: {
    title: 'Not open yet',
    description:
      'This assignment is already visible but not open for submissions yet.',
    color: 'orange',
    iconType: 'hourglass'
  },
  [AssignmentStatus.Open]: {
    title: 'Open',
    description: 'The assignment is open for submissions.',
    color: 'green',
    iconType: 'unlock'
  },
  [AssignmentStatus.Closed]: {
    title: 'Closed',
    description:
      'The assignment is closed. You cannot modify your answers anymore.',
    color: 'red',
    iconType: 'lock'
  }
}

const AssignmentStatusTag: React.FC<AssignmentStatusTagProps> = ({
  status
}) => {
  const statusInfo: AssignmentStatusInfo = AssignmentStatusInfoMapping[status]
  if (statusInfo === undefined) {
    return null
  }
  return (
    <Tooltip title={statusInfo.description}>
      <Tag color={statusInfo.color}>
        {statusInfo.iconType ? <Icon type={statusInfo.iconType} /> : undefined}{' '}
        {statusInfo.title}
      </Tag>
    </Tooltip>
  )
}

export default AssignmentStatusTag
