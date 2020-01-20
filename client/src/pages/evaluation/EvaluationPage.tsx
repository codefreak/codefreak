import { Icon, Result, Steps, Tabs } from 'antd'
import React from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import StartEvaluationButton from '../../components/StartEvaluationButton'
import useSubPath from '../../hooks/useSubPath'
import { useGetEvaluationOverviewQuery } from '../../services/codefreak-api'

const { Step } = Steps
const { TabPane } = Tabs

const EvaluationPage: React.FC<{ answerId: string }> = ({ answerId }) => {
  const subPath = useSubPath()
  const result = useGetEvaluationOverviewQuery({ variables: { answerId } })

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
                <StartEvaluationButton
                  answerId={answerId}
                  type="primary"
                  size="large"
                />
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
