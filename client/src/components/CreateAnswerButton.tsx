import { Button, Modal } from 'antd'
import { ButtonProps } from 'antd/lib/button'
import React, { useState } from 'react'
import {
  CreateAnswerMutation,
  Task,
  useCreateAnswerMutation
} from '../generated/graphql'
import TimeLimitTag from './time-limit/TimeLimitTag'

interface CreateAnswerButtonProps
  extends Omit<ButtonProps, 'onClick' | 'loading'> {
  task: Pick<Task, 'id' | 'timeLimit'>
  onAnswerCreated?: (result: CreateAnswerMutation) => void
}

const CreateAnswerButton: React.FC<CreateAnswerButtonProps> = ({
  task,
  onAnswerCreated,
  ...customButtonProps
}) => {
  const [confirmVisible, setConfirmVisible] = useState<boolean>(false)
  const [createAnswer, { loading: creatingAnswer }] = useCreateAnswerMutation()

  const showConfirm = () => setConfirmVisible(true)
  const hideConfirm = () => setConfirmVisible(false)

  const onCreateAnswer = async () => {
    const answerResult = await createAnswer({
      variables: { taskId: task.id }
    })
    if (answerResult.data && onAnswerCreated) {
      await onAnswerCreated(answerResult.data)
    }
    hideConfirm()
  }

  const buttonProps: ButtonProps = {
    icon: 'rocket',
    type: 'primary',
    ...customButtonProps
  }

  if (!task.timeLimit) {
    return (
      <Button
        {...buttonProps}
        loading={creatingAnswer}
        onClick={onCreateAnswer}
      />
    )
  }

  return (
    <>
      <Modal
        title="This task has a time limit!"
        visible={confirmVisible}
        okText="Start now!"
        onOk={onCreateAnswer}
        confirmLoading={creatingAnswer}
        onCancel={hideConfirm}
      >
        This task has a time limit of{' '}
        <TimeLimitTag style={{ marginRight: 0 }} timeLimit={task.timeLimit} />.
        If you start working on the task, you cannot stop the timer! When the
        time is up, you cannot modify your answer anymore.
      </Modal>
      <Button {...buttonProps} onClick={showConfirm} />
    </>
  )
}

export default CreateAnswerButton
