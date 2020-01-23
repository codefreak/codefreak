import { Icon, Result, Steps, Tabs } from 'antd'
import React, { useEffect, useState } from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import StartEvaluationButton from '../../components/StartEvaluationButton'
import usePendingEvaluation from '../../hooks/usePendingEvaluation'
import useSubPath from '../../hooks/useSubPath'
import {
  PendingEvaluationStatus,
  useGetEvaluationOverviewQuery
} from '../../services/codefreak-api'

const { Step } = Steps
const { TabPane } = Tabs

const EvaluationPage: React.FC<{ answerId: string }> = ({ answerId }) => {
  const subPath = useSubPath()
  const result = useGetEvaluationOverviewQuery({ variables: { answerId } })
  const [step, setStep] = useState(0)
  const [extendedSteps, setExtendedSteps] = useState(false)
  const pendingEvaluation = usePendingEvaluation(answerId)

  useEffect(() => {
    if (pendingEvaluation.loading) {
      return
    }
    switch (pendingEvaluation.status) {
      case null:
        setStep(0)
        break
      case PendingEvaluationStatus.Queued:
        setStep(1)
        break
      case PendingEvaluationStatus.Running:
        setStep(2)
        break
      case PendingEvaluationStatus.Finished:
        setStep(3)
        result.refetch()
        break
    }
  }, [pendingEvaluation, setStep])

  useEffect(() => {
    if (result.data && !result.data.answer.latestEvaluation) {
      setExtendedSteps(true) // stays true after refetch
    }
  }, [result.data])

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { answer } = result.data

  return (
    <>
      <div style={{ padding: '0 64px', marginBottom: 16 }}>
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
            icon={step === 1 ? <Icon type="loading" /> : undefined}
            description={
              extendedSteps
                ? 'Wait for free resources on the server'
                : undefined
            }
          />
          <Step
            title="Execute Evaluation"
            icon={step === 2 ? <Icon type="loading" /> : undefined}
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
      </div>
      <Tabs defaultActiveKey={subPath.get()} onChange={subPath.set}>
        <TabPane tab="Latest Evaluation" key="">
          {answer.latestEvaluation ? (
            'Hier könnte Ihr Evaluationsergebis stehen'
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
