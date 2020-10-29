import { Alert, Button, Modal, Tooltip } from 'antd'
import { ButtonProps } from 'antd/lib/button'
import moment from 'moment'
import React, { useState } from 'react'
import {
  Assignment,
  AssignmentStatus,
  CreateAnswerMutation,
  Task,
  useCreateAnswerMutation
} from '../generated/graphql'
import { useServerMoment } from '../hooks/useServerTimeOffset'
import { momentDifferenceToRelTime, secondsToRelTime } from '../services/time'
import TimeLimitTag from './time-limit/TimeLimitTag'

interface CreateAnswerButtonProps
  extends Omit<ButtonProps, 'onClick' | 'loading'> {
  task: Pick<Task, 'id'>
  assignment?: Pick<Assignment, 'deadline' | 'status' | 'timeLimit'>
  onAnswerCreated?: (result: CreateAnswerMutation) => void
}

const CreateAnswerButton: React.FC<CreateAnswerButtonProps> = ({
  task,
  assignment,
  onAnswerCreated,
  ...customButtonProps
}) => {
  const [confirmVisible, setConfirmVisible] = useState<boolean>(false)
  const [createAnswer, { loading: creatingAnswer }] = useCreateAnswerMutation()
  const serverMoment = useServerMoment()

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

  if (assignment && assignment.status !== AssignmentStatus.Open) {
    return (
      <Tooltip title="The assignment is not open!">
        <Button {...buttonProps} disabled />
      </Tooltip>
    )
  }

  const timeLimit = assignment?.timeLimit
  if (!timeLimit) {
    return (
      <Button
        {...buttonProps}
        loading={creatingAnswer}
        onClick={onCreateAnswer}
      />
    )
  }

  const renderEarlyDeadlineWarning = () => {
    // render warning if assignment deadline is before time limit ends
    if (
      assignment?.deadline &&
      serverMoment().add(timeLimit, 's').isAfter(assignment.deadline)
    ) {
      const taskRelTimeLimit = secondsToRelTime(timeLimit)
      const assignmentRelDeadline = momentDifferenceToRelTime(
        moment(assignment.deadline),
        serverMoment()
      )
      return (
        <Alert
          type="warning"
          iconType="warning"
          showIcon
          message={`You only have ${assignmentRelDeadline} left!`}
          description={
            <>
              The deadline of the task is already in {assignmentRelDeadline}.
              You will not have the full {taskRelTimeLimit} time limit of the
              task.
            </>
          }
        />
      )
    }
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
        <p>
          This task has a time limit of{' '}
          <TimeLimitTag style={{ marginRight: 0 }} timeLimit={timeLimit} />. If
          you start working on the task, you cannot stop the timer! When the
          time is up, you cannot modify your answer anymore.
        </p>
        {renderEarlyDeadlineWarning()}
      </Modal>
      <Button {...buttonProps} onClick={showConfirm} />
    </>
  )
}

export default CreateAnswerButton
