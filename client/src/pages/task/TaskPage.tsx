import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Badge } from 'antd'
import React from 'react'
import { Route, Switch, useRouteMatch } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import { createBreadcrumb } from '../../components/DefaultLayout'
import SetTitle from '../../components/SetTitle'
import useIdParam from '../../hooks/useIdParam'
import useSubPath from '../../hooks/useSubPath'
import { useGetTaskQuery } from '../../services/codefreak-api'
import { createRoutes } from '../../services/custom-breadcrump'
import AnswerPage from '../answer/AnswerPage'
import NotFoundPage from '../NotFoundPage'
import TaskDetailsPage from './TaskDetailsPage'

const TaskPage: React.FC = () => {
  const { path } = useRouteMatch()
  const result = useGetTaskQuery({
    variables: { id: useIdParam() }
  })
  const subPath = useSubPath()

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { task } = result.data

  const tabs = [
    { key: '', tab: 'Task' },
    { key: '/answer', tab: 'Answer' },
    {
      key: '/evaluation',
      tab: (
        <>
          Evaluation <Badge style={{ marginLeft: 4 }} status="processing" />
        </>
      )
    }
  ]

  return (
    <>
      <SetTitle>{task.title}</SetTitle>
      <PageHeaderWrapper
        title={task.title}
        breadcrumb={createBreadcrumb(createRoutes.forTask(task))}
        tabList={tabs}
        tabActiveKey={subPath.get()}
        onTabChange={subPath.set}
      />
      <Switch>
        <Route exact path={path} component={TaskDetailsPage} />
        <Route path={`${path}/answer`}>
          <AnswerPage answerId={task.answer.id}/>{' '}
        </Route>
        <Route component={NotFoundPage} />
      </Switch>
    </>
  )
}

export default TaskPage
