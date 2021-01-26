import React from 'react'
import { Button } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { getEntityPath } from '../services/entity-path'
import { messageService } from '../services/message'
import { useHistory } from 'react-router-dom'
import { useCreateAssignmentMutation } from '../generated/graphql'

const CreateAssignmentButton = () => {
  const history = useHistory()
  const [createAssignmentMutation] = useCreateAssignmentMutation()

  const createAssignment = async () => {
    const result = await createAssignmentMutation()
    if (result.data) {
      const assignment = result.data.createAssignment
      history.push(getEntityPath(assignment))
      messageService.success('Assignment created')
    }
  }

  const createAssignmentButton = (
    <Button icon={<PlusOutlined />} type="primary" onClick={createAssignment}>
      Create Assignment
    </Button>
  )

  return <>{createAssignmentButton}</>
}

export default CreateAssignmentButton
