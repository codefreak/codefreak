import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Badge, Button } from 'antd'
import React, { useEffect, useState } from 'react'
import { Route, Switch, useRouteMatch } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import { createBreadcrumb } from '../../components/DefaultLayout'
import SetTitle from '../../components/SetTitle'
import useIdParam from '../../hooks/useIdParam'
import useSubPath from '../../hooks/useSubPath'
import {
  GetTaskQueryHookResult,
  useCreateAnswerMutation,
  useGetTaskQuery
} from '../../services/codefreak-api'
import { createRoutes } from '../../services/custom-breadcrump'
import AnswerPage from '../answer/AnswerPage'
import NotFoundPage from '../NotFoundPage'
import TaskDetailsPage from './TaskDetailsPage'

const TaskPage: React.FC = () => {
  const { path } = useRouteMatch()
  const subPath = useSubPath()

  const result = useGetTaskQuery({
    variables: { id: useIdParam() }
  })

  const [answer, setAnswer] = useState<
    NonNullable<GetTaskQueryHookResult['data']>['task']['answer']
  >()

  const [
    createAnswer,
    { loading: creatingAnswer, data: createAnswerResult }
  ] = useCreateAnswerMutation()

  useEffect(() => {
    if (result.data && result.data.task.answer) {
      setAnswer(result.data.task.answer)
    }
    if (createAnswerResult) {
      setAnswer(createAnswerResult.createAnswer)
      subPath.set('/answer')
    }
  }, [result, createAnswerResult])

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { task } = result.data

  const tabs = [
    { key: '', tab: 'Task' },
    { key: '/answer', tab: 'Answer', disabled: !answer },
    {
      key: '/evaluation',
      disabled: !answer,
      tab: (
        <>
          Evaluation <Badge style={{ marginLeft: 4 }} status="processing" />
        </>
      )
    }
  ]

  const onCreateAnswer = () => createAnswer({ variables: { taskId: task.id } })

  const extra = answer ? null : (
    <Button
      type="primary"
      icon="rocket"
      size="large"
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
        <Route component={NotFoundPage} />
      </Switch>
    </>
  )
}

export default TaskPage
