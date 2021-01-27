import InlineError from '../components/InlineError'
import React, { SetStateAction, useState } from 'react'

export function useInlineErrorMessage(
  errorTitle: string
): [JSX.Element | null, React.Dispatch<SetStateAction<string>>] {
  const [errorMessage, setErrorMessage] = useState<string>('')
  const inlineError =
    errorMessage.length > 0 ? (
      <InlineError title={errorTitle} message={errorMessage} />
    ) : null

  return [inlineError, setErrorMessage]
}
