import Button, { ButtonProps } from 'antd/lib/button'
import React from 'react'
import { useStartEvaluationMutation } from '../services/codefreak-api'

interface StartEvaluationButtonProps extends ButtonProps {
  answerId: string
}

const StartEvaluationButton: React.FC<StartEvaluationButtonProps> = props => {
  const { answerId, ...restProps } = props

  const [startEvaluation, result] = useStartEvaluationMutation({
    variables: { answerId }
  })

  return (
    <Button
      icon="caret-right"
      onClick={startEvaluation as () => void}
      loading={result.loading}
      {...restProps}
    >
      Start Evaluation
    </Button>
  )
}

export default StartEvaluationButton
