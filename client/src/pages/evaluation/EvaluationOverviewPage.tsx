import { Alert, Icon, Result, Steps, Tabs } from 'antd'
import React, { useContext, useEffect, useState } from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import EvaluationHistory from '../../components/EvaluationHistory'
import EvaluationResult from '../../components/EvaluationResult'
import StartEvaluationButton from '../../components/StartEvaluationButton'
import usePendingEvaluation from '../../hooks/usePendingEvaluation'
import useSubPath from '../../hooks/useSubPath'
import {
  EvaluationStepStatus,
  useGetEvaluationOverviewQuery
} from '../../services/codefreak-api'
import { shorten } from '../../services/short-id'
import { DifferentUserContext } from '../task/TaskPage'

const { Step } = Steps
const { TabPane } = Tabs

const EvaluationPage: React.FC<{
  answerId: string
}> = ({ answerId }) => {
  const subPath = useSubPath()
  const result = useGetEvaluationOverviewQuery({ variables: { answerId } })
  const [step, setStep] = useState(0)
  const [extendedSteps, setExtendedSteps] = useState(false)
  const pendingEvaluation = usePendingEvaluation(answerId, result.refetch)
  const differentUser = useContext(DifferentUserContext)

  useEffect(() => {
    if (pendingEvaluation.loading) {
      return
    }
    switch (pendingEvaluation.status) {
      case null:
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
  }, [pendingEvaluation, setStep, result])

  useEffect(() => {
    if (result.data && !result.data.answer.latestEvaluation) {
      setExtendedSteps(true) // stays true after refetch
    }
  }, [result.data])

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { answer } = result.data
  const onTabChange = (activeKey: string) => {
    subPath.set(
      activeKey,
      differentUser ? { user: shorten(differentUser.id) } : undefined
    )
  }

  return (
    <>
      {!differentUser ? (
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
      ) : null}
      <Tabs defaultActiveKey={subPath.get()} onChange={onTabChange}>
        <TabPane tab="Latest Evaluation" key="">
          {answer.latestEvaluation ? (
            <EvaluationResult evaluationId={answer.latestEvaluation.id} />
          ) : !differentUser ? (
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
          ) : (
            <Alert type="info" message="Answer has not been evaluated, yet" />
          )}
        </TabPane>
        <TabPane
          tab="Evaluation History"
          disabled={!answer.latestEvaluation}
          key="/history"
        >
          <EvaluationHistory answerId={answer.id} />
        </TabPane>
      </Tabs>
    </>
  )
}

export default EvaluationPage
