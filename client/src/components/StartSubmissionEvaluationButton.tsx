import { Button, message } from 'antd'
import { ButtonProps } from 'antd/es/button'
import React, { useEffect } from 'react'
import { useStartAssignmentEvaluationMutation } from '../generated/graphql'

interface StartSubmissionEvaluationButton extends ButtonProps {
  assignmentId: string
  invalidateAll?: boolean
  invalidateTask?: string
}

const StartSubmissionEvaluationButton: React.FC<StartSubmissionEvaluationButton> = ({
  assignmentId,
  invalidateAll,
  invalidateTask,
  ...buttonProps
}) => {
  const [
    startAssignmentEvaluation,
    assignmentEvaluationResult
  ] = useStartAssignmentEvaluationMutation()

  const evaluateAll = () =>
    startAssignmentEvaluation({
      variables: { assignmentId, invalidateTask, invalidateAll }
    })

  useEffect(() => {
    if (assignmentEvaluationResult.data) {
      const {
        startAssignmentEvaluation: queuedEvaluations
      } = assignmentEvaluationResult.data
      if (queuedEvaluations.length) {
        message.success(`Queued ${queuedEvaluations.length} evaluation(s)`)
      } else {
        message.info(`No new evaluations queued`)
      }
    }
  }, [assignmentEvaluationResult])

  return (
    <Button
      {...buttonProps}
      loading={assignmentEvaluationResult.loading}
      onClick={evaluateAll}
    />
  )
}

export default StartSubmissionEvaluationButton
