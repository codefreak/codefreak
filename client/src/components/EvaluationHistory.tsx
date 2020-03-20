import { ApolloProvider, useApolloClient } from '@apollo/react-hooks'
import { Button, Card, Modal, Timeline } from 'antd'
import ApolloClient from 'apollo-client'
import React from 'react'
import {
  EvaluationStepResult,
  GetEvaluationHistoryQueryResult,
  useGetEvaluationHistoryQuery
} from '../generated/graphql'
import usePendingEvaluation from '../hooks/usePendingEvaluation'
import AsyncPlaceholder from './AsyncContainer'
import './EvaluationHistory.less'
import EvaluationResult from './EvaluationResult'
import { EvaluationErrorIcon } from './Icons'

const EvaluationHistory: React.FC<{ answerId: string }> = ({ answerId }) => {
  const result = useGetEvaluationHistoryQuery({ variables: { answerId } })
  const apolloClient = useApolloClient()

  const pendingEvaluation = usePendingEvaluation(answerId, result.refetch)

  const isPending =
    pendingEvaluation.status === 'RUNNING' ||
    pendingEvaluation.status === 'QUEUED'

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const {
    answer: { evaluations }
  } = result.data

  return (
    <Card className="evaluation-history">
      <Timeline reverse pending={isPending ? 'Running...' : undefined}>
        {evaluations.map(renderEvaluation(apolloClient))}
      </Timeline>
    </Card>
  )
}

const renderEvaluation = (apolloClient: ApolloClient<any>) => (
  evaluation: NonNullable<
    GetEvaluationHistoryQueryResult['data']
  >['answer']['evaluations'][0]
) => {
  const showDetails = () =>
    Modal.info({
      icon: null,
      content: (
        <ApolloProvider client={apolloClient}>
          <EvaluationResult evaluationId={evaluation.id} />
        </ApolloProvider>
      ),
      width: 800,
      maskClosable: true
    })

  return (
    <Timeline.Item
      key={evaluation.id}
      {...getDot(evaluation.stepsResultSummary)}
    >
      {new Date(evaluation.createdAt).toLocaleString()}{' '}
      <Button size="small" onClick={showDetails}>
        Details
      </Button>
    </Timeline.Item>
  )
}

const getDot = (result: EvaluationStepResult) => {
  switch (result) {
    case 'SUCCESS':
      return { color: 'green' }
    case 'FAILED':
      return { color: 'red' }
    case 'ERRORED':
      return { dot: <EvaluationErrorIcon style={{ fontSize: '16px' }} /> }
  }
}

export default EvaluationHistory
