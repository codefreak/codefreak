import { Button, notification } from 'antd'
import React, { useEffect } from 'react'
import { useHistory } from 'react-router-dom'
import { useEvaluationFinishedSubscription } from '../generated/graphql'
import { getEntityPath } from '../services/entity-path'

const useSubscribeToGlobalEvents = () => {
  useSubscribeToEvaluationFinished()
}

export default useSubscribeToGlobalEvents

const useSubscribeToEvaluationFinished = () => {
  const history = useHistory()
  const evaluationFinished = useEvaluationFinishedSubscription({
    onSubscriptionData: res => {
      if (res.subscriptionData.data) {
        const evaluation = res.subscriptionData.data.evaluationFinished
        const openResults = () => history.push(getEntityPath(evaluation))
        notification.success({
          message: `Evaluation for ${evaluation.answer.task.title} finished`,
          description: <Button onClick={openResults}>See results</Button>
        })
      }
    }
  })
}
