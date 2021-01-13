import React from 'react'
import { FeedbackSeverity } from '../generated/graphql'
import {
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  WarningOutlined
} from '@ant-design/icons'

const severityIconMap: Record<FeedbackSeverity, React.ElementType> = {
  [FeedbackSeverity.Info]: InfoCircleOutlined,
  [FeedbackSeverity.Minor]: WarningOutlined,
  [FeedbackSeverity.Major]: ExclamationCircleOutlined,
  [FeedbackSeverity.Critical]: CloseCircleOutlined
}

interface FeedbackSeverityIconProps {
  severity: FeedbackSeverity
}

const FeedbackSeverityIcon: React.FC<FeedbackSeverityIconProps> = props => {
  const { severity } = props
  const IconType = severityIconMap[severity]
  const severityClass = severity ? severity.toString().toLowerCase() : 'default'
  return (
    <IconType
      className={`feedback-icon feedback-icon-severity-${severityClass}`}
    />
  )
}

export default FeedbackSeverityIcon
