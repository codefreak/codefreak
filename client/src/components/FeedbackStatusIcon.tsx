import React from 'react'
import { FeedbackStatus } from '../generated/graphql'
import { Icon } from 'antd'

interface FeedbackStatusIcon {
  status: FeedbackStatus
}

const statusIconMap: Record<FeedbackStatus, string> = {
  [FeedbackStatus.Failed]: 'exclamation-circle',
  [FeedbackStatus.Success]: 'check-circle',
  [FeedbackStatus.Ignore]: 'forward'
}

const FeedbackStatusIcon: React.FC<FeedbackStatusIcon> = props => {
  const { status } = props

  const iconType = statusIconMap[status]
  const className = `feedback-icon-${status.toLowerCase()}`

  return <Icon type={iconType} className={`feedback-icon ${className}`} />
}

export default FeedbackStatusIcon
