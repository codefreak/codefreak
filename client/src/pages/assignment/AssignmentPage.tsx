import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Alert, Button, Checkbox, Modal } from 'antd'
import { CheckboxValueType } from 'antd/lib/checkbox/Group'
import React, { useState } from 'react'
import { Route, Switch, useHistory, useRouteMatch } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import Authorized from '../../components/Authorized'
import { createBreadcrumb } from '../../components/DefaultLayout'
import SetTitle from '../../components/SetTitle'
import useHasAuthority from '../../hooks/useHasAuthority'
import useIdParam from '../../hooks/useIdParam'
import useSubPath from '../../hooks/useSubPath'
import {
  Assignment,
  GetTaskListDocument,
  useAddTasksToAssignmentMutation,
  useGetAssignmentQuery,
  useGetTaskPoolForAddingQuery
} from '../../services/codefreak-api'
import { createRoutes } from '../../services/custom-breadcrump'
import { getEntityPath } from '../../services/entity-path'
import { messageService } from '../../services/message'
import NotFoundPage from '../NotFoundPage'
import SubmissionListPage from '../submission/SubmissionListPage'
import TaskListPage from '../task/TaskListPage'

const AssignmentPage: React.FC = () => {
  const { path } = useRouteMatch()
  const result = useGetAssignmentQuery({
    variables: { id: useIdParam() }
  })
  const subPath = useSubPath()

  const tabs = [{ key: '', tab: 'Tasks' }]
  if (useHasAuthority('ROLE_TEACHER')) {
    tabs.push({ key: '/submissions', tab: 'Submissions' })
  }

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { assignment } = result.data

  return (
    <>
      <SetTitle>{assignment.title}</SetTitle>
      <PageHeaderWrapper
        title={assignment.title}
        tabList={tabs}
        tabActiveKey={subPath.get()}
        breadcrumb={createBreadcrumb(createRoutes.forAssignment(assignment))}
        onTabChange={subPath.set}
        extra={
          <Authorized condition={assignment.editable}>
            <AddTasksButton assignment={assignment} />
          </Authorized>
        }
      />
      <Switch>
        <Route exact path={path} component={TaskListPage} />
        <Route path={`${path}/submissions`} component={SubmissionListPage} />
        <Route component={NotFoundPage} />
      </Switch>
    </>
  )
}

const AddTasksButton: React.FC<{
  assignment: Pick<Assignment, 'id' | 'title'>
}> = ({ assignment }) => {
  const [modalVisible, setModalVisible] = useState(false)
  const showModal = () => setModalVisible(true)
  const hideModal = () => setModalVisible(false)
  const [taskIds, setTaskIds] = useState<string[]>([])
  const [addTasks, addTasksResult] = useAddTasksToAssignmentMutation()
  const history = useHistory()
  const submit = async () => {
    await addTasks({
      variables: { assignmentId: assignment.id, taskIds },
      refetchQueries: [
        {
          query: GetTaskListDocument,
          variables: { assignmentId: assignment.id }
        }
      ]
    })
    hideModal()
    messageService.success('Tasks added')
    const tasksPath = getEntityPath(assignment)
    if (history.location.pathname !== tasksPath) {
      history.push(tasksPath)
    }
  }
  return (
    <>
      <Button type="primary" icon="plus" onClick={showModal}>
        Add Tasks
      </Button>
      <Modal
        visible={modalVisible}
        width={700}
        onCancel={hideModal}
        title={`Add tasks to ${assignment.title}`}
        okButtonProps={{
          disabled: taskIds.length === 0,
          loading: addTasksResult.loading
        }}
        onOk={submit}
      >
        <Alert
          message={
            'When a task from the pool is added to an assignment, an independend copy is created. ' +
            'Editing the task in the pool will have no effect on the assignment and vice versa.'
          }
          style={{ marginBottom: 16 }}
        />
        <TaskSelection value={taskIds} setValue={setTaskIds} />
      </Modal>
    </>
  )
}

const TaskSelection: React.FC<{
  value: string[]
  setValue: (value: string[]) => void
}> = props => {
  const result = useGetTaskPoolForAddingQuery()

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const options = result.data.taskPool.map(task => ({
    label: task.title,
    value: task.id
  }))

  const onChange = (value: CheckboxValueType[]) =>
    props.setValue(value as string[])

  return (
    <Checkbox.Group
      className="vertical-checkbox-group"
      options={options}
      onChange={onChange}
      value={props.value}
    />
  )
}

export default AssignmentPage
