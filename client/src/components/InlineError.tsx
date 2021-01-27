import { ReactNode } from 'react'
import { Alert } from 'antd'

type InlineErrorProps = {
  title: string | ReactNode
  message: string | ReactNode
}

const InlineError = (props: InlineErrorProps) => {
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
