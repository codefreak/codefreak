import { Button, Input, Modal } from 'antd'
import React, { useState } from 'react'
import { extractTargetValue } from '../services/util'

const EditableTitle: React.FC<{
  editable: boolean
  title: string
  onChange: (newTitle: string) => void
}> = props => {
  const [modalVisible, setModalVisible] = useState(false)
  const [newTitle, setNewTitle] = useState<string>()
  const showModal = () => {
    setNewTitle(props.title)
    setModalVisible(true)
  }
  const hideModal = () => setModalVisible(false)
  const submit = () => {
    if (newTitle && newTitle.trim()) {
      props.onChange(newTitle.trim())
      hideModal()
    }
  }
  if (!props.editable) {
    return <>{props.title}</>
  }
  return (
    <>
      {props.title}
      <Button icon="edit" type="link" onClick={showModal} />
      <Modal
        visible={modalVisible}
        onCancel={hideModal}
        title="Edit title"
        okButtonProps={{
          disabled: !newTitle || !newTitle.trim()
        }}
        onOk={submit}
      >
        {modalVisible ? ( // re-create for autoFocus
          <Input
            onPressEnter={submit}
            autoFocus
            value={newTitle}
            onChange={extractTargetValue(setNewTitle)}
          />
        ) : null}
      </Modal>
    </>
  )
}

export default EditableTitle
