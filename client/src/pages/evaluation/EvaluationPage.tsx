import { Button, Empty, Steps } from 'antd'
import React from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import {
  useGetEvaluationOverviewQuery,
  useStartEvaluationMutation
} from '../../services/codefreak-api'

const { Step } = Steps

const EvaluationPage: React.FC<{ answerId: string }> = ({ answerId }) => {
  const result = useGetEvaluationOverviewQuery({ variables: { answerId } })

  const [startEvaluation, startEvaluationResult] = useStartEvaluationMutation({
    variables: { answerId }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { answer } = result.data

  return (
    <>
      <Steps current={0}>
        <Step title="Work on Task" />
        <Step
          title="In Progress"
          subTitle="Left 00:00:08"
          description="This is a description."
        />
        <Step title="Waiting" description="This is a description." />
      </Steps>
      <Button
        type="primary"
        size="large"
        icon="caret-right"
        onClick={startEvaluation as () => void}
        loading={startEvaluationResult.loading}
      >
        Start Evaluation
      </Button>

      <br />
      <br />
      <h2>Latest Evaluation</h2>
      {answer.latestEvaluation ? (
        'TODO'
      ) : (
        <Empty description="The evaluation has not been run yet" />
      )}
    </>
  )
}

export default EvaluationPage
