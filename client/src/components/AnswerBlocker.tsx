import { Icon, Result } from 'antd'
import { Moment } from 'moment'
import React, { PropsWithChildren, useEffect, useState } from 'react'

interface AnswerBlockerProps {
  deadline: Moment | undefined
}

const AnswerBlocker: React.FC<PropsWithChildren<AnswerBlockerProps>> = ({
  deadline,
  children
}) => {
  const [deadlineReached, setDeadlineReached] = useState<boolean>(
    deadline !== undefined && deadline.isAfter()
  )

  useEffect(() => {
    const timer = setInterval(() => {
      if (deadline) {
        setDeadlineReached(deadline.isAfter())
      } else {
        clearInterval(timer)
      }
    }, 100)

    return () => {
      clearInterval(timer)
    }
  }, [setDeadlineReached, deadline])

  if (!deadline || deadlineReached) {
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
