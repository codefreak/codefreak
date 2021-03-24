import { Button, Input, Modal } from 'antd'
import React, { useState } from 'react'
import { extractTargetValue } from '../../services/util'
import useAuthenticatedUser from '../../hooks/useAuthenticatedUser'
import {
  useGetUserAliasByUserIdQuery,
  UserAliasInput,
  useUpdateUserAliasMutation
} from '../../generated/graphql'
import AsyncPlaceholder from '../AsyncContainer'
import { messageService } from '../../services/message'

const EditNickname: React.FC<{
  editable: boolean
  onChange: any
}> = props => {
  const [modalVisible, setModalVisible] = useState(false)
  const [newAlias, setNewAlias] = useState<string>()

  const userId = useAuthenticatedUser().id

  const result = useGetUserAliasByUserIdQuery({
    variables: { userId }
  })

  const [updateUserAlias] = useUpdateUserAliasMutation({
    onCompleted: () => {
      result.refetch()
      messageService.success('Nickname may have Updated')
    },
    onError: () => {
      messageService.error('Nickname already exists')
    }
  })

  if (result.data === null || result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }
  const userAlias = result.data.userAliasByUserId

  const input: UserAliasInput = {
    id: userAlias.id,
    alias: newAlias ? newAlias : userAlias.alias
  }

  const showModal = () => {
    setNewAlias(userAlias.alias)
    setModalVisible(true)
  }
  const hideModal = () => setModalVisible(false)

  const submit = () => {
    if (newAlias && newAlias.trim()) {
      props.onChange()
      hideModal()
      return updateUserAlias({ variables: { input } })
    }
  }
  if (!props.editable) {
    return <>{userAlias.alias}</>
  }
  return (
    <>
      <div className={'edit-alias-caption'}>
        <h2>Your Nickname</h2>
        {userAlias.alias}
        <Button icon="edit" type="link" onClick={showModal} />
      </div>
      <Modal
        visible={modalVisible}
        onCancel={hideModal}
        title="Edit Nickname"
        okButtonProps={{
          disabled: !newAlias || !newAlias.trim()
        }}
        onOk={submit}
      >
        {modalVisible ? ( // re-create for autoFocus
          <Input
            onPressEnter={submit}
            autoFocus
            value={newAlias}
            onChange={extractTargetValue(setNewAlias)}
          />
        ) : null}
      </Modal>
    </>
  )
}

export default EditNickname
