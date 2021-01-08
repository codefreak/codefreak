import Button, { ButtonProps } from 'antd/lib/button'
import React from 'react'
import {
  EvaluationStepStatus,
  useStartEvaluationMutation
} from '../services/codefreak-api'
import useEvaluationStatus from '../hooks/useEvaluationStatus'
import { CaretRightOutlined } from '@ant-design/icons'

interface StartEvaluationButtonProps extends ButtonProps {
  answerId: string
}

const StartEvaluationButton: React.FC<StartEvaluationButtonProps> = props => {
  const { answerId, ...restProps } = props

  const status = useEvaluationStatus(answerId)

  const [start, startResult] = useStartEvaluationMutation({
    variables: { answerId }
  })

  return (
    <Button
      icon={<CaretRightOutlined />}
      onClick={start as () => void}
      loading={startResult.loading}
      disabled={
        status === EvaluationStepStatus.Queued ||
        status === EvaluationStepStatus.Running
      }
      {...restProps}
    >
      Start Evaluation
    </Button>
  )
}

export default StartEvaluationButton
