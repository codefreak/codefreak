import React, { useState } from 'react'
import { Button, Input, Modal } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { getEntityPath } from '../services/entity-path'
import { messageService } from '../services/message'
import { useHistory } from 'react-router-dom'
import {
  CreateAssignmentMutationResult,
  useCreateAssignmentMutation,
  useUpdateAssignmentMutation
} from '../generated/graphql'
import { extractTargetValue } from '../services/util'

type Assignment = NonNullable<
  CreateAssignmentMutationResult['data']
>['createAssignment']

const CreateAssignmentButton = () => {
  const history = useHistory()
  const [createEmptyAssignment] = useCreateAssignmentMutation()
  const [updateAssignment] = useUpdateAssignmentMutation()
  const [modalVisible, setModalVisible] = useState(false)
  const showModal = () => setModalVisible(true)
  const hideModal = () => setModalVisible(false)
  const [title, setTitle] = useState<string>('')

  const createAssignment = async () => {
    createEmptyAssignment().then(result => {
      if (result.data) {
        const assignment = result.data.createAssignment
        updateAssignmentTitle(assignment, title)
      }
    })
  }

  const updateAssignmentTitle = (assignment: Assignment, title: string) => {
    updateAssignment({
      variables: {
        id: assignment.id,
        title,
        active: false
      }
    }).then(r => {
      const updatedSuccessfully = r.data && r.data.updateAssignment
      updatedSuccessfully && redirectToAssignmentPage(assignment)
    })
  }

  const redirectToAssignmentPage = (assignment: Assignment) => {
    history.push(getEntityPath(assignment))
    messageService.success('Assignment created')
  }

  const titleInput = modalVisible ? ( // re-create for autoFocus
    <Input
      onPressEnter={createAssignment}
      autoFocus
      value={title}
      placeholder='Set the title of the assignment here'
      onChange={extractTargetValue(setTitle)}
    />
  ) : null

  const createAssignmentButton = (
    <Button icon={<PlusOutlined />} type="primary" onClick={showModal}>
      Create Assignment
    </Button>
  )

  const modal = (
    <Modal
      title="Create assignment"
      visible={modalVisible}
      onOk={createAssignment}
      onCancel={hideModal}
    >
      {titleInput}
    </Modal>
  )

  return (
    <>
      {createAssignmentButton}
      {modal}
    </>
  )
}

export default CreateAssignmentButton
