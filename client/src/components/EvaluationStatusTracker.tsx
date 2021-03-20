import React, { useEffect, useState } from 'react'
import { Steps } from 'antd'
import { EvaluationStepStatus } from '../generated/graphql'
import useAnswerEvaluation from '../hooks/useAnswerEvaluation'
import { LoadingOutlined } from '@ant-design/icons'

const { Step } = Steps

interface EvaluationStatusTrackerProps {
  answerId: string
}

const EvaluationStatusTracker: React.FC<EvaluationStatusTrackerProps> = props => {
  const { answerId } = props
  const { evaluationStatus, latestEvaluation } = useAnswerEvaluation(answerId)

  const [step, setStep] = useState(0)

  useEffect(() => {
    switch (evaluationStatus) {
      case undefined:
        setStep(0)
        break
      case EvaluationStepStatus.Queued:
        setStep(1)
        break
      case EvaluationStepStatus.Running:
        setStep(2)
        break
      case EvaluationStepStatus.Finished:
        setStep(3)
        break
    }
  }, [evaluationStatus, setStep])

  const extendedSteps = !latestEvaluation

  return (
    <Steps current={step} size={extendedSteps ? 'default' : 'small'}>
      <Step
        title="Work on Task"
        description={
          extendedSteps
            ? 'Edit the source code using the online IDE or on your local machine'
            : undefined
        }
      />
      <Step
        title="Queue"
        icon={step === 1 ? <LoadingOutlined /> : undefined}
        description={
          extendedSteps ? 'Wait for free resources on the server' : undefined
        }
      />
      <Step
        title="Execute Evaluation"
        icon={step === 2 ? <LoadingOutlined /> : undefined}
        description={
          extendedSteps
            ? 'Run a set of checks on your proposed solution'
            : undefined
        }
      />
      <Step
        title="Inspect Results"
        description={
          extendedSteps
            ? 'Find out if you solved the task successfully or how you can improve your code'
            : undefined
        }
      />
    </Steps>
  )
}

export default EvaluationStatusTracker
