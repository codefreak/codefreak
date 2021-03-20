import { Alert, Card, Empty } from 'antd'
import { InfoCircleTwoTone } from '@ant-design/icons'
import ReactMarkdown from 'react-markdown'
import React from 'react'
import { useGetTaskDetailsQuery } from '../../generated/graphql'
import useIdParam from '../../hooks/useIdParam'
import AsyncPlaceholder from '../../components/AsyncPlaceholder'
import useHasAuthority from '../../hooks/useHasAuthority'

const TaskInstructionsPage: React.FC = () => {
  const isTeacher = useHasAuthority('ROLE_TEACHER')
  const result = useGetTaskDetailsQuery({
    variables: { id: useIdParam() }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { task } = result.data

  return (
    <>
      {isTeacher ? (
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
      ) : (
        <></>
      )}
      <Card title="Instructions">
        {task.body ? (
          <ReactMarkdown source={task.body} />
        ) : (
          <Empty description="This task has no extra instructions. Take a look at the provided files." />
        )}
      </Card>
    </>
  )
}

export default TaskInstructionsPage
