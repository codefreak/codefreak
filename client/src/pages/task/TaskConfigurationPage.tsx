import { SaveFilled, SyncOutlined } from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Col,
  Empty,
  Input,
  Row,
  Tabs
} from 'antd'
import { CheckboxChangeEvent } from 'antd/lib/checkbox'
import { createRef, useState } from 'react'
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
import EditEvaluationPage from '../evaluation/EditEvaluationPage'
import EditableStringList from '../../components/EditableStringList'
import { TextAreaRef } from 'antd/es/input/TextArea'

const { TabPane } = Tabs

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

const TaskConfigurationPage: React.FC<{ editable: boolean }> = ({
  editable
}) => {
  const subPath = useSubPath()
  const result = useGetTaskDetailsQuery({
    variables: { id: useIdParam(), teacher: editable }
  })
  const runCommandRef = createRef<TextAreaRef>()

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
    protectedFiles: task.protectedFiles,
    defaultFiles: task.defaultFiles,
    customWorkspaceImage: task.customWorkspaceImage,
    runCommand: task.runCommand
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
        <Card title="Workspace Settings" style={{ marginTop: 16 }}>
          <Row gutter={16}>
            <Col span={8}>
              <h3>Run Command</h3>
              <Input.TextArea
                ref={runCommandRef}
                placeholder={`#!/bin/bash\necho "Running main.py..."\npython main.py`}
                defaultValue={task.runCommand ?? ''}
                rows={5}
                style={{ marginBottom: 8 }}
              />
              <Button
                icon={<SaveFilled />}
                onClick={() => {
                  updater('runCommand')(
                    runCommandRef.current?.resizableTextArea?.textArea?.value
                  )
                }}
                type="primary"
              >
                Save Run Command
              </Button>
            </Col>
            <Col span={8}>
              <EditableStringList
                dataSource={task.defaultFiles ?? []}
                onChangeValue={updater('protectedFiles')}
                title="Default editor files"
                tooltipHelp="List of files that will be initially opened in the editor"
                editHelp="List of files that will be initially opened in the editor"
              />
            </Col>
            <Col span={8}>
              <h3>Custom Workspace Image</h3>
              <Alert
                style={{ marginBottom: 16 }}
                type="warning"
                message="Warning: This is an advanced feature and should be used rarely."
              />
              <Input.Search
                defaultValue={task.customWorkspaceImage || ''}
                placeholder="Leave this blank to use the default image (recommended)"
                allowClear
                onSearch={updater('customWorkspaceImage')}
                enterButton={<SaveFilled />}
              />
            </Col>
          </Row>
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
              <EditableStringList
                dataSource={task.hiddenFiles}
                onChangeValue={updater('hiddenFiles')}
                title="Hidden files"
                tooltipHelp="Patterns of files that should be hidden from students. Matching files are only included for evaluation. If matching files are created by students, they are ignored for evaluation."
                editHelp={filePatternHelp}
              />
            </Col>
            <Col span={12}>
              <EditableStringList
                dataSource={task.protectedFiles}
                onChangeValue={updater('protectedFiles')}
                title="Protected files"
                tooltipHelp="Patterns of files that should be read-only. Students will be able to see matching files but modifications are ignored for evaluation. Non-existent files can be protected to prevent their creation."
                editHelp={filePatternHelp}
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

export default TaskConfigurationPage
