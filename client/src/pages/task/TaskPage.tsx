import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { CloudOutlined, FileTextOutlined } from '@ant-design/icons'
import { Switch as AntSwitch, Tooltip } from 'antd'
import { TagType } from 'antd/es/tag'
import moment from 'moment'
import { createContext, useCallback, useState } from 'react'
import {
  Redirect,
  Route,
  Switch,
  useHistory,
  useRouteMatch
} from 'react-router-dom'
import AnswerBlocker from '../../components/AnswerBlocker'
import ArchiveDownload from '../../components/ArchiveDownload'
import AssignmentStatusTag from '../../components/AssignmentStatusTag'
import AsyncPlaceholder from '../../components/AsyncContainer'
import { createBreadcrumb } from '../../components/DefaultLayout'
import EditableTitle from '../../components/EditableTitle'
import SetTitle from '../../components/SetTitle'
import StartEvaluationButton from '../../components/StartEvaluationButton'
import TimeLimitTag from '../../components/time-limit/TimeLimitTag'
import useAssignmentStatusChange from '../../hooks/useAssignmentStatusChange'
import useHasAuthority from '../../hooks/useHasAuthority'
import useIdParam from '../../hooks/useIdParam'
import { useQueryParam } from '../../hooks/useQuery'
import useSubPath from '../../hooks/useSubPath'
import {
  FileContextType,
  GetTaskListDocument,
  GetTaskPoolDocument,
  Maybe,
  PublicUserFieldsFragment,
  TaskInput,
  useCreateAnswerMutation,
  useDeleteAnswerMutation,
  useGetTaskQuery,
  useUpdateTaskMutation
} from '../../services/codefreak-api'
import { BASE_PATHS, getEntityPath } from '../../services/entity-path'
import { messageService } from '../../services/message'
import { unshorten } from '../../services/short-id'
import { displayName } from '../../services/user'
import { makeUpdater, noop } from '../../services/util'
import NotFoundPage from '../NotFoundPage'
import TaskConfigurationPage from './TaskConfigurationPage'
import { useCreateRoutes } from '../../hooks/useCreateRoutes'
import { withTrailingSlash } from '../../services/strings'
import {
  NO_ANSWER_ID,
  NO_AUTH_TOKEN,
  NO_BASE_URL,
  WorkspaceContext,
  WorkspaceContextType
} from '../../hooks/workspace/useWorkspace'
import WorkspacePage from '../../components/workspace/WorkspacePage'
import { Client, createClient } from 'graphql-ws'
import { graphqlWebSocketPath } from '../../services/workspace'
import WorkspaceRunButton from '../../components/workspace/WorkspaceRunButton'
import { UploadAnswerPageButton } from '../answer/UploadAnswerPage'
import CreateAnswerButton from '../../components/CreateAnswerButton'
import { DangerZoneButton } from '../answer/DangerZone'
import useIsWorkspaceAvailableQuery from '../../hooks/workspace/useIsWorkspaceAvailableQuery'

export const DifferentUserContext =
  createContext<PublicUserFieldsFragment | undefined>(undefined)

const tab = (title: string, icon: React.ReactNode) => (
  <>
    {icon} {title}
  </>
)

const TaskPage: React.FC = () => {
  const { path, url } = useRouteMatch()
  const subPath = useSubPath()
  const isTeacher = useHasAuthority('ROLE_TEACHER')
  const userId = useQueryParam('user')
  const history = useHistory()
  const createRoutes = useCreateRoutes()

  const [baseUrl, setBaseUrl] = useState(NO_BASE_URL)
  const [authToken, setAuthToken] = useState<string>()
  const [graphqlWebSocketClient, setGraphqlWebSocketClient] = useState<Client>()
  const [runProcessId, setRunProcessId] = useState<string>()
  const isWorkspaceAvailable = useIsWorkspaceAvailableQuery(baseUrl, authToken)

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
    subPath.set('/ide')
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
      if (!answer) return

      setBaseUrl(NO_BASE_URL)
      setAuthToken(undefined)
      setRunProcessId(undefined)
      // Don't error when active connections are closed
      graphqlWebSocketClient?.on('closed', noop)
      graphqlWebSocketClient?.dispose()
      setGraphqlWebSocketClient(undefined)

      deleteAnswer({
        variables: { id: answer.id },
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
    editable && !differentUser ? (
      <span
        style={{ cursor: 'default', color: 'rgba(0, 0, 0, 0.65)' }}
        key="testing-mode-switch"
      >
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
    ) : null

  const tabs = editable
    ? [
        {
          key: '/configuration',
          tab: tab('Configuration', <FileTextOutlined />)
        },
        {
          key: '/ide',
          tab: tab('Online IDE', <CloudOutlined />),
          disabled: !answer
        }
      ]
    : []

  const assignment = task.assignment
  const submission = assignment?.submission

  const teacherControls: React.ReactNode[] =
    editable && !differentUser
      ? [
          testingModeSwitch,
          <ArchiveDownload url={task.exportUrl} key="export-task-button">
            Export Task
          </ArchiveDownload>
        ]
      : []

  let buttons: React.ReactNode[] = []
  if (differentUser && assignment) {
    // no buttons in the teacher's submission view
    buttons = []
  } else if (answer) {
    // regular buttons to work on task for students
    buttons = [
      <DangerZoneButton
        answer={answer}
        onReset={noop}
        key="danger-zone-button"
      />,
      <UploadAnswerPageButton
        answerId={answer.id}
        key="upload-answer-button"
      />,
      <StartEvaluationButton
        answerId={answer.id}
        type="primary"
        size="large"
        key="start-evaluation-button"
      />,
      <WorkspaceRunButton
        onRunProcessStarted={setRunProcessId}
        key="workspace-run-button"
      />
    ]
  }

  const title = differentUser
    ? `${task.title} – ${displayName(differentUser)}`
    : task.title

  const onTabChange = (activeKey: string) => {
    subPath.set(activeKey, userId ? { user: userId } : undefined)
  }

  const submissionDeadline = submission?.deadline
    ? moment(submission.deadline)
    : undefined

  const renderTimeLimit = () => {
    return assignment?.timeLimit ? (
      <TimeLimitTag
        timeLimit={assignment.timeLimit}
        deadline={submissionDeadline}
        key="time-limit-tag"
      />
    ) : undefined
  }

  const tags = [
    assignment ? (
      <AssignmentStatusTag
        status={assignment.status}
        key="assignment-status-tag"
      />
    ) : undefined,
    renderTimeLimit()
  ].filter((it): it is React.ReactElement<TagType> => it !== undefined)

  const goToAssignment = () => {
    let previousPath

    if (assignment && differentUser) {
      previousPath = getEntityPath(assignment) + '/submissions'
    } else if (assignment) {
      previousPath = getEntityPath(assignment)
    } else {
      previousPath = BASE_PATHS.Task + '/pool'
    }

    history.push(previousPath)
  }

  const handleBaseUrlChange = (
    newBaseUrl: string,
    newAuthToken?: Maybe<string>
  ) => {
    if (newBaseUrl === NO_BASE_URL) {
      return
    }

    setBaseUrl(withTrailingSlash(newBaseUrl))
    setAuthToken(newAuthToken ?? NO_AUTH_TOKEN)
    setGraphqlWebSocketClient(
      createClient({
        url: graphqlWebSocketPath(newBaseUrl),
        lazy: false,
        connectionParams: newAuthToken ? { jwt: newAuthToken } : undefined,
        // Close the connection after one hour of inactivity
        lazyCloseTimeout: 3_600_000
      })
    )
  }

  const workspaceContext: WorkspaceContextType = {
    isAvailable: isWorkspaceAvailable,
    baseUrl,
    authToken,
    answerId: answer?.id ?? NO_ANSWER_ID,
    taskId: task.id,
    graphqlWebSocketClient,
    runProcessId
  }

  const createAnswerButton = (
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

  return (
    <DifferentUserContext.Provider value={differentUser}>
      <WorkspaceContext.Provider value={workspaceContext}>
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
          extra={[...teacherControls, ...buttons]}
          onBack={goToAssignment}
        />
        <Switch>
          <Route exact path={path}>
            {editable ? (
              <Redirect to={`${url}/configuration`} />
            ) : (
              <Redirect to={`${url}/ide`} />
            )}
          </Route>
          <Route path={`${path}/configuration`}>
            {editable ? (
              <TaskConfigurationPage editable={editable} />
            ) : (
              <NotFoundPage />
            )}
          </Route>
          <Route path={`${path}/ide`}>
            {answer || teacherControls.length === 0 ? (
              <div className="no-padding">
                <AnswerBlocker deadline={submissionDeadline}>
                  <WorkspacePage
                    type={FileContextType.Answer}
                    onBaseUrlChange={handleBaseUrlChange}
                    createAnswerButton={createAnswerButton}
                    initialOpenFiles={task.defaultFiles ?? []}
                  />
                </AnswerBlocker>
              </div>
            ) : (
              <NotFoundPage />
            )}
          </Route>
          <Route component={NotFoundPage} />
        </Switch>
      </WorkspaceContext.Provider>
    </DifferentUserContext.Provider>
  )
}

export default TaskPage
