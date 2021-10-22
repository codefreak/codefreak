import Button, { ButtonProps } from 'antd/lib/button'
import React from 'react'
import {
  GetLatestEvaluationDocument,
  useStartEvaluationMutation
} from '../services/codefreak-api'
import useEvaluationStatus from '../hooks/useEvaluationStatus'
import { isEvaluationInProgress } from '../services/evaluation'
import { CaretRightOutlined } from '@ant-design/icons'

interface StartEvaluationButtonProps extends ButtonProps {
  answerId: string
}

const StartEvaluationButton: React.FC<StartEvaluationButtonProps> = props => {
  const { answerId, ...restProps } = props

  const status = useEvaluationStatus(answerId)

  const [start, startResult] = useStartEvaluationMutation({
    variables: { answerId },
    refetchQueries: [
      {
        query: GetLatestEvaluationDocument,
        variables: { answerId }
      }
    ]
  })

  return (
    <Button
      icon={<CaretRightOutlined />}
      onClick={start as () => void}
      loading={startResult.loading}
      disabled={isEvaluationInProgress(status)}
      {...restProps}
    >
      Evaluate
    </Button>
  )
}

export default StartEvaluationButton
