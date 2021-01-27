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
import { extractTargetValue, noop } from '../services/util'
import { useInlineErrorMessage } from '../hooks/useInlineErrorMessage'

type Assignment = NonNullable<
  CreateAssignmentMutationResult['data']
>['createAssignment']

const CreateAssignmentButton = () => {
  const history = useHistory()
  const [
    createEmptyAssignment,
    { loading: creating }
  ] = useCreateAssignmentMutation({
    context: { disableGlobalErrorHandling: true }
  })
  const [updateAssignment, { loading: updating }] = useUpdateAssignmentMutation(
    {
      context: { disableGlobalErrorHandling: true }
    }
  )
  const [modalVisible, setModalVisible] = useState(false)
  const showModal = () => setModalVisible(true)
  const hideModal = () => setModalVisible(false)
  const [title, setTitle] = useState<string>('')
  const [inlineError, setErrorMessage] = useInlineErrorMessage(
    'Error while creating assignment'
  )
  const okButtonDisabled = title.length === 0

  const createAssignment = async () => {
    createEmptyAssignment()
      .then(result => {
        if (result.data) {
          const assignment = result.data.createAssignment
          updateAssignmentTitle(assignment, title)
        }
      })
      .catch(error => setErrorMessage(error.message))
  }

  const updateAssignmentTitle = (assignment: Assignment, newTitle: string) => {
    updateAssignment({
      variables: {
        id: assignment.id,
        title: newTitle,
        active: false
      }
    })
      .then(result => {
        const updatedSuccessfully = result.data && result.data.updateAssignment
        if (updatedSuccessfully) {
          redirectToAssignmentPage(assignment)
        }
      })
      .catch(error => setErrorMessage(error.message))
  }

  const redirectToAssignmentPage = (assignment: Assignment) => {
    history.push(getEntityPath(assignment))
    messageService.success('Assignment created')
  }

  const titleInput = modalVisible ? ( // re-create for autoFocus
    <Input
      onPressEnter={okButtonDisabled ? noop : createAssignment}
      autoFocus
      value={title}
      placeholder="Set the title of the assignment here"
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
      okButtonProps={{
        disabled: okButtonDisabled,
        loading: creating || updating,
        title: okButtonDisabled ? 'The assignment needs a title!' : undefined
      }}
      onCancel={hideModal}
    >
      {inlineError}
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
