import React from 'react'
import { FeedbackSeverity } from '../generated/graphql'
import { Icon } from 'antd'

const severityIconMap: Record<FeedbackSeverity, string> = {
  [FeedbackSeverity.Info]: 'info-circle',
  [FeedbackSeverity.Minor]: 'warning',
  [FeedbackSeverity.Major]: 'exclamation-circle',
  [FeedbackSeverity.Critical]: 'close-circle'
}

interface FeedbackSeverityIconProps {
  severity: FeedbackSeverity
}

const FeedbackSeverityIcon: React.FC<FeedbackSeverityIconProps> = props => {
  const { severity } = props
  let iconType = 'question-circle'
  if (severity && severityIconMap[severity]) {
    iconType = severityIconMap[severity]
  }
  const severityClass = severity ? severity.toString().toLowerCase() : 'default'
  return (
    <Icon
      type={iconType}
      className={`feedback-icon feedback-icon-severity-${severityClass}`}
    />
  )
}

export default FeedbackSeverityIcon
