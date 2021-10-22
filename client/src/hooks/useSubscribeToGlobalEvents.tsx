import { Button, notification } from 'antd'
import { useHistory } from 'react-router-dom'
import { useEvaluationFinishedSubscription } from '../generated/graphql'
import { getEntityPath } from '../services/entity-path'
import { RIGHT_TAB_QUERY_PARAM } from '../components/workspace/WorkspacePage'
import { WorkspaceTabType } from '../services/workspace-tabs'

const useSubscribeToGlobalEvents = () => {
  useSubscribeToEvaluationFinished()
}

export default useSubscribeToGlobalEvents

const useSubscribeToEvaluationFinished = () => {
  const history = useHistory()
  useEvaluationFinishedSubscription({
    onSubscriptionData: res => {
      if (res.subscriptionData.data) {
        const { evaluation } = res.subscriptionData.data.evaluationStatusUpdated
        const openResults = () => {
          history.push(
            getEntityPath(evaluation.answer.task) +
              '/ide?' +
              RIGHT_TAB_QUERY_PARAM +
              '=' +
              WorkspaceTabType.EVALUATION
          )
        }
        notification.success({
          message: `Evaluation for ${evaluation.answer.task.title} finished`,
          description: <Button onClick={openResults}>See results</Button>
        })
      }
    }
  })
}
