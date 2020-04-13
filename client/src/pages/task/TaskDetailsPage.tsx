import { Alert, Button, Card, Col, Empty, Icon, List, Row, Tooltip } from 'antd'
import { JSONSchema6 } from 'json-schema'
import React from 'react'
import { Link } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import EditableMarkdown from '../../components/EditableMarkdown'
import JsonSchemaEditButton from '../../components/JsonSchemaEditButton'
import useIdParam from '../../hooks/useIdParam'
import {
  TaskDetailsInput,
  useGetTaskDetailsQuery,
  useUpdateTaskDetailsMutation
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'
import { shorten } from '../../services/short-id'
import { makeUpdater } from '../../services/util'

const renderFilePattern = (pattern: string) => (
  <List.Item>
    <code>{pattern}</code>
  </List.Item>
)

const filePatternSchema: JSONSchema6 = {
  type: 'array',
  items: { type: 'string' }
}

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

const TaskDetailsPage: React.FC<{ editable: boolean }> = ({ editable }) => {
  const result = useGetTaskDetailsQuery({
    variables: { id: useIdParam(), teacher: editable }
  })

  const [updateMutation] = useUpdateTaskDetailsMutation({
    onCompleted: () => {
      result.refetch()
      messageService.success('Task updated')
    }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { task } = result.data

  const taskDetailsInput: TaskDetailsInput = {
    id: task.id,
    hiddenFiles: task.hiddenFiles,
    protectedFiles: task.protectedFiles
  }

  const updater = makeUpdater(taskDetailsInput, input =>
    updateMutation({ variables: { input } })
  )

  return (
    <>
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
      {editable ? (
        <Card title="Files" style={{ marginTop: 16 }}>
          {task.assignment && task.assignment.status === 'OPEN' ? (
            <Alert
              style={{ marginBottom: 16 }}
              message="Warning"
              description="The assignment is already open. If you make changes to files, they are not applied to already created answers. Every change that is saved will apply to newly created answers. This can happen automatically, for example when the IDE is idle."
              type="warning"
              showIcon
            />
          ) : null}
          <p>
            <Link
              to={'/ide/task/' + shorten(task.id)}
              target={'task-ide-' + task.id}
            >
              <Button type="primary" icon="edit">
                Open in IDE
              </Button>
            </Link>
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
                        title={
                          'Patterns of files that should be hidden from students. Matching files are only included for evaluation. If matching files are created by students, they are ignored for evaluation.'
                        }
                        placement="bottom"
                      >
                        <Icon type="info-circle" theme="filled" />
                      </Tooltip>
                    </span>
                    <JsonSchemaEditButton
                      title="Edit hidden files"
                      extraContent={filePatternHelp}
                      schema={filePatternSchema}
                      value={task.hiddenFiles}
                      onSubmit={updater('hiddenFiles')}
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
                        title={
                          'Patterns of files that should be read-only. Students will be able to see matching files but modifications are ignored for evaluation. Non-existent files can be protected to prevent their creation.'
                        }
                        placement="bottom"
                      >
                        <Icon type="info-circle" theme="filled" />
                      </Tooltip>
                    </span>
                    <JsonSchemaEditButton
                      title="Edit protected files"
                      extraContent={filePatternHelp}
                      schema={filePatternSchema}
                      value={task.protectedFiles}
                      onSubmit={updater('protectedFiles')}
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
      ) : null}
    </>
  )
}

export default TaskDetailsPage
