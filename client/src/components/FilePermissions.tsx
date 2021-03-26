import { Alert, Col, List, Row, Tooltip } from 'antd'
import { InfoCircleFilled } from '@ant-design/icons'
import JsonSchemaEditButton from './JsonSchemaEditButton'
import { JSONSchema6 } from 'json-schema'

const FilePermissions = (props: any) => {
  const { task, updater } = props

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
          File patterns use the Ant pattern syntax. For more information refer
          to the{' '}
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

  return (
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
                  <InfoCircleFilled />
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
                  <InfoCircleFilled />
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
  )
}

export default FilePermissions
