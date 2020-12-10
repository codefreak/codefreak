import React, { ReactNode } from 'react'
import { Alert } from 'antd'

type InineErrorProps = {
  title: string | ReactNode
  message: string | ReactNode
}

const InlineError = (props: InineErrorProps) => {
  // Wrap error message to retain formatting
  const formattedErrorMessage = (
    <code>
      <pre>{props.message}</pre>
    </code>
  )

  return (
    <Alert
      message={props.title}
      description={formattedErrorMessage}
      type="error"
      showIcon
      style={{ marginBottom: 16 }}
    />
  )
}

export default InlineError
