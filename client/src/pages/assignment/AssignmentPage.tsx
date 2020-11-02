import { PageHeaderWrapper } from '@ant-design/pro-layout'
import {
  Alert,
  Button,
  Checkbox,
  Col,
  DatePicker,
  Descriptions,
  Dropdown,
  Form,
  Menu,
  Modal,
  Row,
  Steps,
  TimePicker
} from 'antd'
import { DropdownButtonProps } from 'antd/es/dropdown/dropdown-button'
import { CheckboxValueType } from 'antd/lib/checkbox/Group'
import moment, { Moment, unitOfTime } from 'moment'
import React, { useCallback, useState } from 'react'
import { Route, Switch, useHistory, useRouteMatch } from 'react-router-dom'
import { debounce } from 'ts-debounce'
import ArchiveDownload from '../../components/ArchiveDownload'
import AssignmentStatusTag from '../../components/AssignmentStatusTag'
import AsyncPlaceholder from '../../components/AsyncContainer'
import Authorized from '../../components/Authorized'
import { createBreadcrumb } from '../../components/DefaultLayout'
import EditableTitle from '../../components/EditableTitle'
import SetTitle from '../../components/SetTitle'
import CopyToClipboardButton from '../../components/CopyToClipboardButton'
import TimeIntervalInput, {
  TimeIntervalInputProps
} from '../../components/TimeIntervalInput'
import useAssignmentStatusChange from '../../hooks/useAssignmentStatusChange'
import { useFormatter } from '../../hooks/useFormatter'
import useHasAuthority from '../../hooks/useHasAuthority'
import useIdParam from '../../hooks/useIdParam'
import { useServerMoment } from '../../hooks/useServerTimeOffset'
import useSubPath from '../../hooks/useSubPath'
import {
  Assignment,
  AssignmentStatus,
  GetTaskListDocument,
  UpdateAssignmentMutationVariables,
  useAddTasksToAssignmentMutation,
  useGetAssignmentQuery,
  useGetTaskPoolForAddingQuery,
  useUpdateAssignmentMutation
} from '../../services/codefreak-api'
import { createRoutes } from '../../services/custom-breadcrump'
import { getEntityPath } from '../../services/entity-path'
import { messageService } from '../../services/message'
import {
  componentsToSeconds,
  momentToIsoCb,
  secondsToComponents
} from '../../services/time'
import { makeUpdater, noop, Updater } from '../../services/util'
import NotFoundPage from '../NotFoundPage'
import SubmissionListPage from '../submission/SubmissionListPage'
import TaskListPage from '../task/TaskListPage'
import './AssignmentPage.less'
import SortSelect from '../../components/SortSelect'
import SearchBar from '../../components/SearchBar'
import {
  filterTasks,
  TaskSortMethods,
  TaskSortMethodNames
} from '../../services/task'

const { Step } = Steps

const activeStep = {
  INACTIVE: 0,
  ACTIVE: 1,
  OPEN: 2,
  CLOSED: 3
}

const AssignmentPage: React.FC = () => {
  const assignmentId = useIdParam()
  const { path } = useRouteMatch()
  const result = useGetAssignmentQuery({
    variables: { id: assignmentId }
  })
  const subPath = useSubPath()
  const formatter = useFormatter()
  const [updateMutation] = useUpdateAssignmentMutation({
    onCompleted: () => {
      result.refetch()
      messageService.success('Assignment updated')
    }
  })

  useAssignmentStatusChange(
    assignmentId,
    useCallback(() => {
      result.refetch()
    }, [result])
  )

  const tabs = [{ key: '', tab: 'Tasks' }]
  if (useHasAuthority('ROLE_TEACHER')) {
    tabs.push({ key: '/submissions', tab: 'Submissions' })
  }

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { assignment } = result.data

  const assignmentInput: UpdateAssignmentMutationVariables = {
    id: assignment.id,
    title: assignment.title,
    active: assignment.active,
    deadline: assignment.deadline,
    openFrom: assignment.openFrom,
    timeLimit: assignment.timeLimit
  }

  const updater = makeUpdater(assignmentInput, variables =>
    updateMutation({ variables })
  )

  const renderDate = (
    label: string,
    onOk: (date?: string) => any,
    value?: string | null
  ) => {
    const handleClear = (v: any) => (v === null ? onOk() : noop())
    return assignment.editable ? (
      <Descriptions.Item label={label}>
        <DatePicker
          key={'' + value}
          showTime
          onChange={handleClear}
          defaultValue={value ? moment(value) : undefined}
          onOk={momentToIsoCb(onOk)}
        />
      </Descriptions.Item>
    ) : value ? (
      <Descriptions.Item label={label}>
        {formatter.dateTime(value)}
      </Descriptions.Item>
    ) : null
  }

  const updateTimeLimit = updater('timeLimit')
  const onTimeLimitChange: TimeIntervalInputProps['onChange'] = async limit => {
    const seconds = limit ? componentsToSeconds(limit) : 0
    await updateTimeLimit(seconds > 0 ? seconds : null)
  }

  return (
    <>
      <SetTitle>{assignment.title}</SetTitle>
      <PageHeaderWrapper
        title={
          <EditableTitle
            editable={assignment.editable}
            title={assignment.title}
            onChange={updater('title')}
          />
        }
        tags={<AssignmentStatusTag status={assignment.status} />}
        tabList={tabs}
        tabActiveKey={subPath.get()}
        breadcrumb={createBreadcrumb(createRoutes.forAssignment(assignment))}
        onTabChange={subPath.set}
        extra={
          <Authorized condition={assignment.editable}>
            <CopyToClipboardButton icon="link" value={window.location.href}>
              Share Assignment Link
            </CopyToClipboardButton>
            <ArchiveDownload url={assignment.exportUrl}>
              Export Assignment
            </ArchiveDownload>
            <AddTasksButton assignment={assignment} />
          </Authorized>
        }
        content={
          <>
            <Descriptions className="assignment-dates" size="small" column={4}>
              <Descriptions.Item label="Created">
                {formatter.date(assignment.createdAt)}
              </Descriptions.Item>
              {renderDate(
                'Open From',
                updater('openFrom'),
                assignment.openFrom
              )}
              {renderDate('Deadline', updater('deadline'), assignment.deadline)}
              <Descriptions.Item label="Time Limit">
                <TimeIntervalInput
                  onChange={debounce(onTimeLimitChange, 500)}
                  defaultValue={
                    assignment.timeLimit
                      ? secondsToComponents(assignment.timeLimit)
                      : undefined
                  }
                  nullable
                />
              </Descriptions.Item>
            </Descriptions>
            <Authorized condition={assignment.editable}>
              <StatusSteps
                input={assignmentInput}
                mutation={updateMutation}
                status={assignment.status}
                updater={updater}
              />
            </Authorized>
          </>
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

const StatusSteps: React.FC<{
  status: AssignmentStatus
  updater: Updater<UpdateAssignmentMutationVariables>
  input: UpdateAssignmentMutationVariables
  mutation: (args: { variables: UpdateAssignmentMutationVariables }) => any
}> = props => {
  const { status, updater, mutation, input } = props
  const [stepsExpanded, setStepsExpanded] = useState(false)
  const toggleStepsExpanded = () => setStepsExpanded(!stepsExpanded)

  const closeNow = () =>
    mutation({
      variables: { ...input, deadline: moment().toISOString() }
    })

  const activate = () => updater('active')(true)
  const deactivate = () => updater('active')(false)

  return (
    <div className="statusSteps">
      <Button
        onClick={toggleStepsExpanded}
        icon={stepsExpanded ? 'caret-down' : 'caret-right'}
        size="small"
        style={{ marginRight: 16, marginTop: 4 }}
      />
      <div
        style={{ flexGrow: 1 }}
        className={stepsExpanded ? 'wrapper' : 'wrapper notExpanded'}
        onClick={stepsExpanded ? undefined : toggleStepsExpanded}
      >
        <Steps size="small" current={activeStep[status]}>
          <Step
            title="Inactive"
            description={
              stepsExpanded ? (
                <>
                  <p>The assignment is not public yet. Only you can see it.</p>
                  <Button disabled={status === 'INACTIVE'} onClick={deactivate}>
                    Deactivate
                  </Button>
                </>
              ) : undefined
            }
          />
          <Step
            title="Active"
            description={
              stepsExpanded ? (
                <>
                  <p>
                    The assignment is public. Students can see it but not work
                    on it yet.
                  </p>
                  <Button
                    type={status === 'INACTIVE' ? 'primary' : 'default'}
                    disabled={status === 'ACTIVE' || status === 'OPEN'}
                    onClick={activate}
                  >
                    Activate
                  </Button>
                </>
              ) : undefined
            }
          />
          <Step
            title="Open"
            description={
              stepsExpanded ? (
                <>
                  <p>
                    The assignment is open for submissions. If a deadline is
                    set, it will be closed automatically.
                  </p>
                  <OpenAssignmentButton
                    input={input}
                    mutation={mutation}
                    disabled={status === 'OPEN'}
                    type={status === 'ACTIVE' ? 'primary' : undefined}
                  />
                </>
              ) : undefined
            }
          />
          <Step
            title="Closed"
            description={
              stepsExpanded ? (
                <>
                  <p>
                    The assignment is closed. Students can still see it but not
                    change their submissions.
                  </p>
                  <Button
                    onClick={closeNow}
                    disabled={status === 'CLOSED'}
                    type={status === 'OPEN' ? 'primary' : 'default'}
                  >
                    Close Now
                  </Button>
                </>
              ) : undefined
            }
          />
        </Steps>
      </div>
    </div>
  )
}

const AddTasksButton: React.FC<{
  assignment: Pick<Assignment, 'id' | 'title'>
}> = ({ assignment }) => {
  const [modalVisible, setModalVisible] = useState(false)
  const [taskIds, setTaskIds] = useState<string[]>([])
  const showModal = () => {
    setTaskIds([])
    setModalVisible(true)
  }
  const hideModal = () => setModalVisible(false)
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

  const [sortMethod, setSortMethod] = useState(TaskSortMethodNames[0])
  const [filterCriteria, setFilterCriteria] = useState('')

  const handleSortChange = (value: string) => setSortMethod(value)
  const handleFilterChange = (value: string) => setFilterCriteria(value)

  const sorter = (
    <SortSelect
      defaultValue={TaskSortMethodNames[0]}
      values={TaskSortMethodNames}
      onSortChange={handleSortChange}
    />
  )

  const searchBar = (
    <SearchBar
      searchType="Task"
      placeholder="by name..."
      onChange={handleFilterChange}
    />
  )

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
        <Row gutter={16} type="flex">
          <Col>{searchBar}</Col>
          <Col>{sorter}</Col>
        </Row>
        <Alert
          message={
            'When a task from the pool is added to an assignment, an independent copy is created. ' +
            'Editing the task in the pool will have no effect on the assignment and vice versa.'
          }
          style={{ marginBottom: 16, marginTop: 16 }}
        />
        <TaskSelection
          value={taskIds}
          setValue={setTaskIds}
          sortMethod={sortMethod}
          filterCriteria={filterCriteria}
        />
      </Modal>
    </>
  )
}

const TaskSelection: React.FC<{
  value: string[]
  setValue: (value: string[]) => void
  sortMethod: string
  filterCriteria: string
}> = props => {
  const result = useGetTaskPoolForAddingQuery()

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  let taskPool = result.data.taskPool.slice()

  if (props.filterCriteria) {
    taskPool = filterTasks(taskPool, props.filterCriteria)
  }

  taskPool = taskPool.sort(TaskSortMethods[props.sortMethod])

  const options = taskPool.map(task => ({
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

const OpenAssignmentButton: React.FC<
  {
    input: UpdateAssignmentMutationVariables
    mutation: (args: { variables: UpdateAssignmentMutationVariables }) => any
  } & Omit<DropdownButtonProps, 'overlay'>
> = ({ input, mutation, ...buttonProps }) => {
  const [modalVisible, setModalVisible] = useState(false)
  const serverMoment = useServerMoment()
  const [from, setFrom] = useState(serverMoment())
  const [period, setPeriod] = useState(moment('00:30:00', 'HH:mm:ss'))
  const showModal = () => {
    setFrom(serverMoment())
    setPeriod(moment('00:30:00', 'HH:mm:ss'))
    setModalVisible(true)
  }
  const hideModal = () => setModalVisible(false)
  const submit = () => {
    mutation({
      variables: {
        ...input,
        active: true,
        openFrom: from.toISOString(),
        deadline: from
          .add(
            period.hours() * 60 * 60 + period.minutes() * 60 + period.seconds(),
            'seconds'
          )
          .toISOString()
      }
    })
    hideModal()
  }

  const isInPast = (date: Moment | null, resolution?: unitOfTime.StartOf) =>
    (date && date.isBefore(serverMoment(), resolution)) || false

  const openNow = () => {
    const variables = {
      ...input,
      active: true,
      openFrom: moment().toISOString()
    }
    if (variables.deadline && isInPast(moment(variables.deadline))) {
      delete variables.deadline
    }
    mutation({ variables })
  }

  const onChangeDate = (date: Moment | null) => date && setFrom(date)
  const isBeforeToday = (date: Moment | null) => isInPast(date, 'days')
  return (
    <>
      <Dropdown.Button
        style={{ marginRight: 8 }}
        onClick={openNow}
        overlay={
          <Menu>
            <Menu.Item key="1" onClick={showModal}>
              Open for specific period
            </Menu.Item>
          </Menu>
        }
        {...buttonProps}
      >
        Open Now
      </Dropdown.Button>
      <Modal
        visible={modalVisible}
        onCancel={hideModal}
        title={'Open submissions for specific period of time'}
        okButtonProps={{
          disabled: period.minutes() < 1
        }}
        onOk={submit}
      >
        <Form labelCol={{ span: 6 }}>
          <Form.Item label="From">
            <DatePicker
              showTime
              allowClear={false}
              value={from}
              onChange={onChangeDate}
              disabledDate={isBeforeToday}
            />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }} label="For">
            <TimePicker
              allowClear={false}
              onChange={setPeriod}
              value={period}
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}

export default AssignmentPage
