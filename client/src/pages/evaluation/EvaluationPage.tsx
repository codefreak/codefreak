import { Button, Empty, Icon, Result, Steps, Tabs } from 'antd'
import React from 'react'
import { useRouteMatch } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import useSubPath from '../../hooks/useSubPath'
import {
  useGetEvaluationOverviewQuery,
  useStartEvaluationMutation
} from '../../services/codefreak-api'

const { Step } = Steps
const { TabPane } = Tabs

const EvaluationPage: React.FC<{ answerId: string }> = ({ answerId }) => {
  const subPath = useSubPath()
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

      <br />
      <br />
      <Tabs defaultActiveKey={subPath.get()} onChange={subPath.set}>
        <TabPane tab="Latest Evaluation" key="">
          {answer.latestEvaluation ? (
            'TODO'
          ) : (
            <Result
              icon={<Icon type="rocket" theme="twoTone" />}
              title="Wondering if your solution is correct? ✨"
              extra={
                <Button
                  type="primary"
                  size="large"
                  icon="caret-right"
                  onClick={startEvaluation as () => void}
                  loading={startEvaluationResult.loading}
                >
                  Start Evaluation
                </Button>
              }
            />
          )}
        </TabPane>
        <TabPane
          tab="Evaluation History"
          disabled={!answer.latestEvaluation}
          key="/history"
        >
          Content of Tab Pane 2
        </TabPane>
      </Tabs>
    </>
  )
}

export default EvaluationPage
