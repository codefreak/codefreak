import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Button, Icon, Tooltip } from 'antd'
import { Switch as AntSwitch } from 'antd'
import Tag from 'antd/es/tag'
import moment from 'moment'
import React, { createContext, useCallback } from 'react'
import { Redirect, Route, Switch, useRouteMatch } from 'react-router-dom'
import { useHistory } from 'react-router-dom'
import AnswerBlocker from '../../components/AnswerBlocker'
import ArchiveDownload from '../../components/ArchiveDownload'
import AssignmentStatusTag from '../../components/AssignmentStatusTag'
import AsyncPlaceholder from '../../components/AsyncContainer'
import CreateAnswerButton from '../../components/CreateAnswerButton'
import { createBreadcrumb } from '../../components/DefaultLayout'
import EditableTitle from '../../components/EditableTitle'
import EvaluationIndicator from '../../components/EvaluationIndicator'
import IdeIframe from '../../components/IdeIframe'
import SetTitle from '../../components/SetTitle'
import StartEvaluationButton from '../../components/StartEvaluationButton'
import TimeLimitTag from '../../components/time-limit/TimeLimitTag'
import useAssignmentStatusChange from '../../hooks/useAssignmentStatusChange'
import useHasAuthority from '../../hooks/useHasAuthority'
import useIdParam from '../../hooks/useIdParam'
import { useQueryParam } from '../../hooks/useQuery'
import useSubPath from '../../hooks/useSubPath'
import {
  GetTaskListDocument,
  GetTaskPoolDocument,
  PublicUserFieldsFragment,
  TaskInput,
  useCreateAnswerMutation,
  useDeleteAnswerMutation,
  useGetTaskQuery,
  useUpdateTaskMutation
} from '../../services/codefreak-api'
import { createRoutes } from '../../services/custom-breadcrump'
import { getEntityPath } from '../../services/entity-path'
import { messageService } from '../../services/message'
import { unshorten } from '../../services/short-id'
import { displayName } from '../../services/user'
import { makeUpdater } from '../../services/util'
import AnswerPage from '../answer/AnswerPage'
import EvaluationPage from '../evaluation/EvaluationOverviewPage'
import NotFoundPage from '../NotFoundPage'
import TaskDetailsPage from './TaskDetailsPage'

export const DifferentUserContext = createContext<
  PublicUserFieldsFragment | undefined
>(undefined)

const tab = (title: string, icon: string) => (
  <>
    <Icon type={icon} /> {title}
  </>
)

const TaskPage: React.FC = () => {
  const { path, url } = useRouteMatch()
  const subPath = useSubPath()
  const isTeacher = useHasAuthority('ROLE_TEACHER')
  const userId = useQueryParam('user')
  const history = useHistory()

  const result = useGetTaskQuery({
    variables: {
      id: useIdParam(),
      userId: isTeacher && userId ? unshorten(userId) : undefined
    }
  })

  useAssignmentStatusChange(
    result?.data?.task.assignment?.id,
    useCallback(() => {
      result.refetch()
    }, [result])
  )

  const [createAnswer, { loading: creatingAnswer }] = useCreateAnswerMutation()
  const [deleteAnswer, { loading: deletingAnswer }] = useDeleteAnswerMutation()

  const onAnswerCreated = useCallback(() => {
    subPath.set('/answer')
    result.refetch()
  }, [result, subPath])

  const [updateMutation] = useUpdateTaskMutation({
    onCompleted: () => {
      result.refetch()
      messageService.success('Task updated')
    }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }
  const { task } = result.data
  const { answer } = task
  const differentUser =
    isTeacher && answer && userId ? answer.submission.user : undefined
  const editable = task.editable && !differentUser

  const taskInput: TaskInput = {
    id: task.id,
    title: task.title
  }

  const updater = makeUpdater(taskInput, input =>
    updateMutation({ variables: { input } })
  )

  const setTestingMode = (enabled: boolean) => {
    if (enabled) {
      createAnswer({ variables: { taskId: task.id } }).then(onAnswerCreated)
    } else {
      deleteAnswer({
        variables: { id: answer!.id },
        refetchQueries: [
          task.assignment
            ? {
                query: GetTaskListDocument,
                variables: { assignmentId: task.assignment.id }
              }
            : {
                query: GetTaskPoolDocument
              }
        ]
      }).then(() => {
        subPath.set('')
        result.refetch()
      })
    }
  }

  const testingModeSwitch =
    editable && !differentUser
      ? [
          {
            key: 'testing-mode',
            tab: (
              <span style={{ cursor: 'default', color: 'rgba(0, 0, 0, 0.65)' }}>
                Testing Mode{' '}
                <Tooltip
                  placement="right"
                  title="Enable this for testing the automatic evaluation. This will create an answer like students would do. Disabling deletes the answer."
                >
                  <AntSwitch
                    onChange={setTestingMode}
                    style={{ marginLeft: 8 }}
                    checked={answer !== null}
                    loading={creatingAnswer || deletingAnswer}
                  />
                </Tooltip>
              </span>
            )
          }
        ]
      : []

  // insert online IDE before last element if enabled
  const ideTab = task.ideEnabled
    ? [
        {
          key: '/ide',
          tab: tab('Online IDE', 'cloud'),
          disabled: !answer
        }
      ]
    : []

  const tabs = [
    { key: '/details', tab: tab('Task', 'file-text') },
    ...testingModeSwitch,
    { key: '/answer', tab: tab('Answer', 'solution'), disabled: !answer },
    ...ideTab,
    {
      key: '/evaluation',
      disabled: !answer,
      tab: (
        <>
          {tab('Evaluation', 'dashboard')}
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

  const assignment = task.assignment
  const submission = assignment?.submission

  const teacherControls =
    editable && !differentUser ? (
      <ArchiveDownload url={task.exportUrl}>Export Task</ArchiveDownload>
    ) : null

  let buttons
  if (differentUser && assignment) {
    // Show "back to submissions" button for teachers
    const onClick = () =>
      history.push(getEntityPath(assignment) + '/submissions')
    buttons = (
      <Button icon="arrow-left" size="large" onClick={onClick}>
        Back to submissions
      </Button>
    )
  } else if (answer) {
    // regular buttons to work on task for students
    buttons = (
      <StartEvaluationButton answerId={answer.id} type="primary" size="large" />
    )
  } else if (!teacherControls) {
    // start working on task by default
    buttons = (
      <CreateAnswerButton
        size="large"
        task={task}
        assignment={assignment || undefined}
        submission={submission || undefined}
        onAnswerCreated={onAnswerCreated}
      >
        Start working on this task!
      </CreateAnswerButton>
    )
  }

  const title = differentUser
    ? `${task.title} – ${displayName(differentUser)}`
    : task.title

  const onTabChange = (activeKey: string) => {
    if (activeKey !== 'testing-mode') {
      subPath.set(activeKey, userId ? { user: userId } : undefined)
    }
  }

  const renderTimeLimit = () => {
    return assignment?.timeLimit ? (
      <TimeLimitTag
        timeLimit={assignment.timeLimit}
        deadline={
          submission?.deadline ? moment(submission.deadline) : undefined
        }
      />
    ) : undefined
  }

  const tags = [
    assignment ? <AssignmentStatusTag status={assignment.status} /> : undefined,
    renderTimeLimit()
  ].filter((it): it is React.ReactElement<Tag> => it !== undefined)

  return (
    <DifferentUserContext.Provider value={differentUser}>
      <SetTitle>{task.title}</SetTitle>
      <PageHeaderWrapper
        title={
          <EditableTitle
            editable={editable}
            title={title}
            onChange={updater('title')}
          />
        }
        tags={tags}
        breadcrumb={createBreadcrumb(createRoutes.forTask(task))}
        tabList={tabs}
        tabActiveKey={subPath.get()}
        onTabChange={onTabChange}
        extra={
          <>
            {teacherControls} {buttons}
          </>
        }
      />
      <Switch>
        <Route exact path={path}>
          <Redirect to={`${url}/details`} />
        </Route>
        <Route path={`${path}/details`}>
          <TaskDetailsPage editable={editable} />
        </Route>
        <Route path={`${path}/answer`}>
          {answer ? <AnswerPage answerId={answer.id} /> : <NotFoundPage />}
        </Route>
        <Route path={`${path}/evaluation`}>
          {answer ? <EvaluationPage answerId={answer.id} /> : <NotFoundPage />}
        </Route>
        <Route path={`${path}/ide`}>
          {answer ? (
            <div className="no-padding">
              <AnswerBlocker
                deadline={
                  submission?.deadline ? moment(submission.deadline) : undefined
                }
              >
                <IdeIframe type="answer" id={answer.id} />
              </AnswerBlocker>
            </div>
          ) : (
            <NotFoundPage />
          )}
        </Route>
        <Route component={NotFoundPage} />
      </Switch>
    </DifferentUserContext.Provider>
  )
}

export default TaskPage
