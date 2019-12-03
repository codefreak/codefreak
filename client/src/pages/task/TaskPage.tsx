import { PageHeaderWrapper } from '@ant-design/pro-layout'
import React from 'react'
import { createBreadcrumb } from '../../components/DefaultLayout'
import SetTitle from '../../components/SetTitle'
import { createRoutes } from '../../services/custom-breadcrump'

const TaskPage: React.FC = () => {
  const task = {
    __typename: 'Task',
    id: '1337',
    title: 'Sample Task',
    assignment: {
      __typename: 'Assignment',
      id: '42',
      title: 'Sample Assignment'
    }
  } as any
  return (
    <>
      <SetTitle>{task.title}</SetTitle>
      <PageHeaderWrapper
        title={task.title}
        breadcrumb={createBreadcrumb(createRoutes.forTask(task))}
      />
    </>
  )
}

export default TaskPage
