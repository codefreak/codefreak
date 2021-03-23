import React from 'react'
import { FeedbackSeverity, FeedbackStatus } from '../generated/graphql'
import FeedbackSeverityIcon from './FeedbackSeverityIcon'
import FeedbackStatusIcon from './FeedbackStatusIcon'

interface FeedbackIconProps {
  status?: FeedbackStatus
  severity?: FeedbackSeverity
}

const FeedbackIcon: React.FC<FeedbackIconProps> = props => {
  const { status, severity } = props

  if (severity) {
    return <FeedbackSeverityIcon severity={severity} />
  }
  if (status) {
    return <FeedbackStatusIcon status={status} />
  }
  return null
}

export default FeedbackIcon
