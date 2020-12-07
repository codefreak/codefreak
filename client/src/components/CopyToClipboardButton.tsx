import { Button } from 'antd'
import { ButtonProps } from 'antd/es/button'
import React, { useCallback, useEffect, useState } from 'react'
import copyToClipboard from 'copy-to-clipboard'

export interface CopyToClipboardButtonProps extends ButtonProps {
  value: string
  // the Options interface is not exported from the package. welp...
  options?: Parameters<typeof copyToClipboard>[1]
  resetTimeout?: number
  copiedMessage?: React.ReactElement
  copiedIcon?: ButtonProps['icon']
}

const CopyToClipboardButton: React.FC<CopyToClipboardButtonProps> = ({
  value,
  options,
  copiedMessage,
  copiedIcon,
  resetTimeout,
  icon: originalIcon,
  onClick: originalOnClick,
  children: originalChildren,
  ...additionalButtonProps
}) => {
  const [copied, setCopied] = useState<boolean>(false)

  useEffect(() => {
    if (copied) {
      const timeoutId = setTimeout(() => {
        setCopied(false)
      }, resetTimeout ?? 2000)
      return () => clearTimeout(timeoutId)
    }
  }, [resetTimeout, copied, setCopied])

  const onClick = useCallback<React.MouseEventHandler<HTMLElement>>(
    e => {
      copyToClipboard(value, options)
      setCopied(true)
      if (originalOnClick) originalOnClick(e)
    },
    [setCopied, value, originalOnClick, options]
  )

  const buttonProps: ButtonProps = {
    icon: copied ? copiedIcon ?? 'check' : originalIcon ?? 'copy',
    ...additionalButtonProps
  }
  return (
    <Button onClick={onClick} {...buttonProps}>
      {copied ? copiedMessage : originalChildren}
    </Button>
  )
}

export default CopyToClipboardButton
