import { Button, notification } from 'antd'
import { useHistory } from 'react-router-dom'
import { useEvaluationFinishedSubscription } from '../generated/graphql'
import { getEntityPath } from '../services/entity-path'
import {
  LEFT_TAB_QUERY_PARAM,
  RIGHT_TAB_QUERY_PARAM
} from '../components/workspace/WorkspacePage'
import { WorkspaceTabType } from '../services/workspace-tabs'
import { useQueryParam } from './useQuery'

const useSubscribeToGlobalEvents = () => {
  useSubscribeToEvaluationFinished()
}

export default useSubscribeToGlobalEvents

const useSubscribeToEvaluationFinished = () => {
  const history = useHistory()
  const leftWorkspaceTab = useQueryParam(LEFT_TAB_QUERY_PARAM)

  useEvaluationFinishedSubscription({
    onSubscriptionData: res => {
      if (res.subscriptionData.data) {
        const { evaluation } = res.subscriptionData.data.evaluationStatusUpdated
        const openResults = () => {
          history.push(
            getEntityPath(evaluation.answer.task) +
              '/ide?' +
              LEFT_TAB_QUERY_PARAM +
              '=' +
              (leftWorkspaceTab ?? '') +
              '&' +
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
