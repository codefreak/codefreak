import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Button } from 'antd'
import React from 'react'
import { Route, Switch, useRouteMatch } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import { createBreadcrumb } from '../../components/DefaultLayout'
import EvaluationIndicator from '../../components/EvaluationIndicator'
import SetTitle from '../../components/SetTitle'
import StartEvaluationButton from '../../components/StartEvaluationButton'
import useIdParam from '../../hooks/useIdParam'
import useSubPath from '../../hooks/useSubPath'
import {
  useCreateAnswerMutation,
  useGetTaskQuery
} from '../../services/codefreak-api'
import { createRoutes } from '../../services/custom-breadcrump'
import AnswerPage from '../answer/AnswerPage'
import EvaluationPage from '../evaluation/EvaluationPage'
import NotFoundPage from '../NotFoundPage'
import TaskDetailsPage from './TaskDetailsPage'

const TaskPage: React.FC = () => {
  const { path } = useRouteMatch()
  const subPath = useSubPath()

  const result = useGetTaskQuery({
    variables: { id: useIdParam() }
  })

  const [createAnswer, { loading: creatingAnswer }] = useCreateAnswerMutation()

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { task } = result.data
  const { answer } = task
  const pool = !task.assignment

  const answerTab = pool
    ? []
    : [{ key: '/answer', tab: 'Answer', disabled: !answer }]

  const tabs = [
    { key: '', tab: 'Task' },
    ...answerTab,
    {
      key: '/evaluation',
      disabled: !answer,
      tab: (
        <>
          Evaluation
          {answer ? (
            <EvaluationIndicator
              style={{ marginLeft: 8 }}
              answerId={answer.id}
            />
          ) : null}
        </>
      )
    }
  ]

  const onCreateAnswer = async () => {
    const createAnswerResult = await createAnswer({
      variables: { taskId: task.id }
    })
    if (createAnswerResult.data) {
      subPath.set('/answer')
      result.refetch()
    }
  }

  const extra = pool ? null : answer ? (
    <>
      <StartEvaluationButton answerId={answer.id} type="primary" size="large" />
    </>
  ) : (
    <Button
      icon="rocket"
      size="large"
      type="primary"
      onClick={onCreateAnswer}
      loading={creatingAnswer}
    >
      Start working on this task!
    </Button>
  )

  return (
    <>
      <SetTitle>{task.title}</SetTitle>
      <PageHeaderWrapper
        title={task.title}
        breadcrumb={createBreadcrumb(createRoutes.forTask(task))}
        tabList={tabs}
        tabActiveKey={subPath.get()}
        onTabChange={subPath.set}
        extra={extra}
      />
      <Switch>
        <Route exact path={path} component={TaskDetailsPage} />
        <Route path={`${path}/answer`}>
          {answer ? <AnswerPage answerId={answer.id} /> : <NotFoundPage />}
        </Route>
        <Route path={`${path}/evaluation`}>
          {answer ? <EvaluationPage answerId={answer.id} /> : <NotFoundPage />}
        </Route>
        <Route component={NotFoundPage} />
      </Switch>
    </>
  )
}

export default TaskPage
