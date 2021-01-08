import { Alert, Button, Modal, Tooltip } from 'antd'
import { ButtonProps } from 'antd/lib/button'
import moment from 'moment'
import React, { useState } from 'react'
import {
  Assignment,
  AssignmentStatus,
  CreateAnswerMutation,
  Submission,
  Task,
  useCreateAnswerMutation
} from '../generated/graphql'
import { useServerMoment, useServerNow } from '../hooks/useServerTimeOffset'
import { momentDifferenceToRelTime, secondsToRelTime } from '../services/time'
import TimeLimitTag from './time-limit/TimeLimitTag'
import useMomentReached from '../hooks/useMomentReached'

interface CreateAnswerButtonProps
  extends Omit<ButtonProps, 'onClick' | 'loading'> {
  task: Pick<Task, 'id'>
  assignment?: Pick<Assignment, 'deadline' | 'status' | 'timeLimit'>
  submission?: Pick<Submission, 'deadline'>
  onAnswerCreated?: (result: CreateAnswerMutation) => void
}

const CreateAnswerButton: React.FC<CreateAnswerButtonProps> = props => {
  const {
    task,
    assignment,
    submission,
    onAnswerCreated,
    ...customButtonProps
  } = props
  const [confirmVisible, setConfirmVisible] = useState<boolean>(false)
  const [createAnswer, { loading: creatingAnswer }] = useCreateAnswerMutation()
  const serverMoment = useServerMoment()
  const now = useServerNow()
  const deadline = submission?.deadline || assignment?.deadline
  const deadlineReached = useMomentReached(
    deadline ? moment(deadline) : undefined,
    now
  )

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

  if (deadlineReached) {
    return (
      <Tooltip title="The deadline has been reached. You cannot create an answer anymore.">
        <Button {...buttonProps} disabled />
      </Tooltip>
    )
  }

  const timeLimit = assignment?.timeLimit
  // do not ask for confirmation if assignment has
  // no time limit or there is already a submission
  if (!timeLimit || !!submission) {
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
    // only render if no submission exists, yet
    if (
      !submission &&
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
          showIcon
          message={`You only have ${assignmentRelDeadline} left!`}
          description={
            <>
              The deadline of the assignment is already in{' '}
              {assignmentRelDeadline}. You will not have the full{' '}
              {taskRelTimeLimit} time limit of the assignment.
            </>
          }
        />
      )
    }
  }

  return (
    <>
      <Modal
        title="This assignment has a time limit!"
        visible={confirmVisible}
        okText="Start now!"
        onOk={onCreateAnswer}
        confirmLoading={creatingAnswer}
        onCancel={hideConfirm}
      >
        <p>
          This assignment has a time limit of{' '}
          <TimeLimitTag style={{ marginRight: 0 }} timeLimit={timeLimit} />. If
          you start working on the task, you cannot stop the timer! When the
          time is up, you cannot modify your answers anymore.
        </p>
        {renderEarlyDeadlineWarning()}
      </Modal>
      <Button {...buttonProps} onClick={showConfirm} />
    </>
  )
}

export default CreateAnswerButton
