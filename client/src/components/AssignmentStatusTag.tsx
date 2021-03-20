import {
  EyeInvisibleOutlined,
  HourglassOutlined,
  LockOutlined,
  UnlockOutlined
} from '@ant-design/icons'
import { Tag, Tooltip } from 'antd'
import React from 'react'
import { AssignmentStatus } from '../generated/graphql'

interface AssignmentStatusTagProps {
  status: AssignmentStatus
}

interface AssignmentStatusInfo {
  title: string
  description: string
  color?: string
  iconType?: React.ElementType
}

const AssignmentStatusInfoMapping: {
  [key in AssignmentStatus]: AssignmentStatusInfo
} = {
  [AssignmentStatus.Inactive]: {
    title: 'Inactive',
    description: 'This assignment is only visible for you.',
    iconType: EyeInvisibleOutlined
  },
  [AssignmentStatus.Active]: {
    title: 'Not open yet',
    description:
      'This assignment is already visible but not open for submissions yet.',
    color: 'orange',
    iconType: HourglassOutlined
  },
  [AssignmentStatus.Open]: {
    title: 'Open',
    description: 'The assignment is open for submissions.',
    color: 'green',
    iconType: UnlockOutlined
  },
  [AssignmentStatus.Closed]: {
    title: 'Closed',
    description:
      'The assignment is closed. You cannot modify your answers anymore.',
    color: 'red',
    iconType: LockOutlined
  }
}

const AssignmentStatusTag: React.FC<AssignmentStatusTagProps> = ({
  status
}) => {
  const statusInfo: AssignmentStatusInfo = AssignmentStatusInfoMapping[status]
  if (statusInfo === undefined) {
    return null
  }
  const Icon = statusInfo.iconType
  return (
    <Tooltip title={statusInfo.description}>
      <Tag color={statusInfo.color}>
        {Icon ? <Icon /> : undefined} {statusInfo.title}
      </Tag>
    </Tooltip>
  )
}

export default AssignmentStatusTag
