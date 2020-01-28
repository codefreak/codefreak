import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Alert, Button, Card } from 'antd'
import React from 'react'
import FileImport from '../../components/FileImport'

const CreateTaskPage: React.FC = () => {
  return (
    <>
      <PageHeaderWrapper />
      <Alert
        message="Tasks can only be created in the task pool. You can later add them to any assignment."
        style={{ marginBottom: 16 }}
      />
      <Card title="From Template" style={{ marginBottom: 16 }}>
        TODO
      </Card>
      <Card title="Import" style={{ marginBottom: 16 }}>
        <FileImport
          uploading={false}
          onUpload={() => {}}
          importing={false}
          onImport={() => {}}
        />
      </Card>
      <Card title="From Scratch">
        <div style={{ textAlign: 'center' }}>
          <Button size="large" block>
            Create Empty Task
          </Button>
        </div>
      </Card>
    </>
  )
}

export default CreateTaskPage
