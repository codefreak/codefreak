import React from 'react'
import { FeedbackStatus } from '../generated/graphql'
import {
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ForwardOutlined
} from '@ant-design/icons'

interface FeedbackStatusIconProps {
  status: FeedbackStatus
}

const statusIconMap: Record<FeedbackStatus, React.ElementType> = {
  [FeedbackStatus.Failed]: ExclamationCircleOutlined,
  [FeedbackStatus.Success]: CheckCircleOutlined,
  [FeedbackStatus.Ignore]: ForwardOutlined
}

const FeedbackStatusIcon: React.FC<FeedbackStatusIconProps> = props => {
  const { status } = props

  const IconType = statusIconMap[status]
  const className = `feedback-icon-${status.toLowerCase()}`

  return <IconType className={`feedback-icon ${className}`} />
}

export default FeedbackStatusIcon
