import { Icon, Result } from 'antd'
import { Moment } from 'moment'
import React, { PropsWithChildren } from 'react'
import useMomentReached from '../hooks/useMomentReached'

interface AnswerBlockerProps {
  deadline: Moment | undefined
}

const AnswerBlocker: React.FC<PropsWithChildren<AnswerBlockerProps>> = ({
  deadline,
  children
}) => {
  const deadlineReached = useMomentReached(deadline)

  if (deadline === undefined || !deadlineReached) {
    return <>{children}</>
  } else {
    const relativeTime = deadline.fromNow(true)
    return (
      <Result
        status="info"
        title="Time's up!"
        icon={<Icon type="clock-circle" />}
        subTitle={
          <>
            The deadline has been reached {relativeTime} ago. <br />
            You cannot edit your answer anymore.
          </>
        }
      />
    )
  }
}

export default AnswerBlocker
