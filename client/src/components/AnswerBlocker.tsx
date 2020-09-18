import { Icon, Result } from 'antd'
import { Moment } from 'moment'
import React, { PropsWithChildren } from 'react'
import useMomentReached from '../hooks/useMomentReached'
import { useServerNow } from '../hooks/useServerTimeOffset'

interface AnswerBlockerProps {
  deadline: Moment | undefined
}

const AnswerBlocker: React.FC<PropsWithChildren<AnswerBlockerProps>> = ({
  deadline,
  children
}) => {
  const now = useServerNow()
  const deadlineReached = useMomentReached(deadline, now)
  if (!deadline || !deadlineReached) {
    return <>{children}</>
  } else {
    const relativeTime = deadline.from(now())
    return (
      <Result
        status="info"
        title="Time's up!"
        icon={<Icon type="clock-circle" />}
        subTitle={
          <>
            The deadline has been reached {relativeTime}. <br />
            You cannot edit your answer anymore.
          </>
        }
      />
    )
  }
}

export default AnswerBlocker
