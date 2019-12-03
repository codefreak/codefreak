import { PageHeaderWrapper } from '@ant-design/pro-layout'
import React from 'react'
import { Route, Switch, useRouteMatch } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import { createBreadcrumb } from '../../components/DefaultLayout'
import SetTitle from '../../components/SetTitle'
import { useGetAssignmentQuery } from '../../generated/graphql'
import useIdParam from '../../hooks/useIdParam'
import useSubPath from '../../hooks/useSubPath'
import { createRoutes } from '../../services/custom-breadcrump'
import NotFoundPage from '../NotFoundPage'
import SubmissionListPage from '../submission/SubmissionListPage'
import TaskListPage from '../task/TaskListPage'

const AssignmentPage: React.FC = () => {
  const { path } = useRouteMatch()
  const result = useGetAssignmentQuery({
    variables: { id: useIdParam() }
  })
  const subPath = useSubPath()

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { assignment } = result.data

  return (
    <>
      <SetTitle>{assignment.title}</SetTitle>
      <PageHeaderWrapper
        title={assignment.title}
        tabList={[
          { key: '', tab: 'Tasks' },
          { key: '/submissions', tab: 'Submissions' }
        ]}
        tabActiveKey={subPath.get()}
        breadcrumb={createBreadcrumb(createRoutes.forAssignment(assignment))}
        onTabChange={subPath.set}
      />
      <Switch>
        <Route exact path={path} component={TaskListPage} />
        <Route path={`${path}/submissions`} component={SubmissionListPage} />
        <Route component={NotFoundPage} />
      </Switch>
    </>
  )
}

export default AssignmentPage
