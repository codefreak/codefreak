import {
  EditOutlined,
  InfoCircleFilled,
  InfoCircleTwoTone,
  SyncOutlined
} from '@ant-design/icons'
import {
  Alert,
  Card,
  Checkbox,
  Col,
  Empty,
  List,
  Row,
  Tabs,
  Tooltip
} from 'antd'
import { CheckboxChangeEvent } from 'antd/lib/checkbox'
import { useState } from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import EditableMarkdown from '../../components/EditableMarkdown'
import StartSubmissionEvaluationButton from '../../components/StartSubmissionEvaluationButton'
import useIdParam from '../../hooks/useIdParam'
import useSubPath from '../../hooks/useSubPath'
import {
  FileContextType,
  GetTaskDetailsDocument,
  Maybe,
  TaskDetailsInput,
  useGetTaskDetailsQuery,
  useUpdateTaskDetailsMutation
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'
import { makeUpdater } from '../../services/util'
import FileBrowser from '../../components/FileBrowser'
import Markdown from '../../components/Markdown'
import { EditStringArrayButton } from '../../components/EditStringArrayModal'
import EditEvaluationPage from '../evaluation/EditEvaluationPage'

const { TabPane } = Tabs

const renderFilePattern = (pattern: string) => (
  <List.Item>
    <code>{pattern}</code>
  </List.Item>
)

const filePatternHelp = (
  <Alert
    style={{ marginBottom: 16 }}
    message={
      <>
        File patterns use the Ant pattern syntax. For more information refer to
        the{' '}
        <a
          href="https://ant.apache.org/manual/dirtasks.html#patterns"
          target="_blank"
          rel="noopener noreferrer"
        >
          official documentation
        </a>
        .
      </>
    }
    type="info"
  />
)

export const renderTaskInstructionsText = (
  instructions: Maybe<string> | undefined
) => {
  return instructions ? (
    <Markdown>{instructions}</Markdown>
  ) : (
    <Empty description="This task has no extra instructions. Take a look at the provided files." />
  )
}

const TaskDetailsPage: React.FC<{ editable: boolean }> = ({ editable }) => {
  const subPath = useSubPath()
  const result = useGetTaskDetailsQuery({
    variables: { id: useIdParam(), teacher: editable }
  })

  const [updateMutation] = useUpdateTaskDetailsMutation({
    onCompleted: () => {
      messageService.success('Task updated')
    },
    refetchQueries: [
      {
        query: GetTaskDetailsDocument,
        variables: { id: useIdParam(), teacher: editable }
      }
    ]
  })

  const [sureToEditFiles, setSureToEditFiles] = useState(false)
  const onSureToEditFilesChange = (e: CheckboxChangeEvent) =>
    setSureToEditFiles(e.target.checked)

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { task } = result.data

  const details = (
    <Card title="Instructions">{renderTaskInstructionsText(task.body)}</Card>
  )

  // hiddenFiles and protectedFiles are null if task is not editable
  if (!task.hiddenFiles || !task.protectedFiles) {
    return details
  }

  const taskDetailsInput: TaskDetailsInput = {
    id: task.id,
    body: task.body,
    hiddenFiles: task.hiddenFiles,
    protectedFiles: task.protectedFiles
  }

  const updater = makeUpdater(taskDetailsInput, input =>
    updateMutation({ variables: { input } })
  )

  const assignmentOpen = task.assignment?.status === 'OPEN'

  return (
    <Tabs
      defaultActiveKey={subPath.get()}
      onChange={subPath.set}
      style={{ marginTop: -16 }}
    >
      <TabPane tab="Details" key="">
        <Alert
          type="info"
          message={
            <>
              This is what your students will see when they open the task. Check
              out the "edit" tabs that are only visible to you.
              <br />
              <InfoCircleTwoTone /> To try out what your students see when they
              start working on this task, enable <i>testing mode</i>.
            </>
          }
          style={{ marginBottom: 16 }}
        />
        {details}
      </TabPane>
      <TabPane tab="Edit Details" key="/edit">
        <Card title="Instructions">
          {task.body || editable ? (
            <EditableMarkdown
              content={task.body}
              editable={editable}
              onSave={updater('body')}
            />
          ) : (
            <Empty description="This task has no extra instructions. Take a look at the provided files." />
          )}
        </Card>
        <Card title="Files" style={{ marginTop: 16 }}>
          {assignmentOpen ? (
            <Alert
              style={{ marginBottom: 16 }}
              message="Warning"
              description={
                <>
                  <p>
                    The assignment is already open. If you make changes to
                    files, they do not affect students that already started to
                    work on this task. Only students that start the task after
                    the change will get the updated files. Hidden and protected
                    files are updated for everyone but only in new evaluations.
                    Past evaluations are not affected.
                  </p>
                  <p>
                    After you made changes to the files please click the
                    evaluation button to check all answers again!
                  </p>
                  <p>
                    <Checkbox onChange={onSureToEditFilesChange}>
                      I understand this and want to do it anyway
                    </Checkbox>
                  </p>
                </>
              }
              type="warning"
              showIcon
            />
          ) : null}
          <FileBrowser type={FileContextType.Task} id={task.id} />
          <p style={{ marginTop: '1em' }}>
            {task.assignment?.id && (
              <StartSubmissionEvaluationButton
                assignmentId={task.assignment.id}
                invalidateTask={task.id}
                disabled={assignmentOpen && !sureToEditFiles}
                type="primary"
                icon={<SyncOutlined />}
              >
                Evaluate all answers of this task
              </StartSubmissionEvaluationButton>
            )}
          </p>
          <Row gutter={16}>
            <Col span={12}>
              <List
                size="small"
                header={
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}
                  >
                    <span>
                      <b>Hidden files</b>{' '}
                      <Tooltip
                        title="Patterns of files that should be hidden from students. Matching files are only included for evaluation. If matching files are created by students, they are ignored for evaluation."
                        placement="bottom"
                      >
                        <InfoCircleFilled />
                      </Tooltip>
                    </span>
                    <EditStringArrayButton
                      title="Edit hidden files pattern"
                      extraContent={filePatternHelp}
                      initialValues={task.hiddenFiles}
                      onSave={updater('hiddenFiles')}
                      icon={<EditOutlined />}
                      type="link"
                    />
                  </div>
                }
                bordered
                dataSource={task.hiddenFiles}
                renderItem={renderFilePattern}
              />
            </Col>
            <Col span={12}>
              <List
                size="small"
                header={
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}
                  >
                    <span>
                      <b>Protected files</b>{' '}
                      <Tooltip
                        title="Patterns of files that should be read-only. Students will be able to see matching files but modifications are ignored for evaluation. Non-existent files can be protected to prevent their creation."
                        placement="bottom"
                      >
                        <InfoCircleFilled />
                      </Tooltip>
                    </span>
                    <EditStringArrayButton
                      title="Edit protected files pattern"
                      extraContent={filePatternHelp}
                      initialValues={task.protectedFiles}
                      onSave={updater('protectedFiles')}
                      icon={<EditOutlined />}
                      type="link"
                    />
                  </div>
                }
                bordered
                dataSource={task.protectedFiles}
                renderItem={renderFilePattern}
              />
            </Col>
          </Row>
        </Card>
      </TabPane>
      <TabPane tab="Edit Evaluation" key="/edit-evaluation">
        <EditEvaluationPage taskId={task.id} />
      </TabPane>
    </Tabs>
  )
}

export default TaskDetailsPage
