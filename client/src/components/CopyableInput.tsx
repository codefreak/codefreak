import React from 'react'
import { InputProps } from 'antd/es/input'
import { Input, Tooltip } from 'antd'
import CopyToClipboardButton from './CopyToClipboardButton'

interface CopyableInputProps extends InputProps {
  value: string
}

const CopyableInput: React.FC<CopyableInputProps> = props => {
  const { value, ...inputProps } = props

  return (
    <Input.Group compact>
      <Input
        {...inputProps}
        style={{ width: 'calc(100% - 32px)' }}
        readOnly
        value={value}
      />
      <Tooltip title="Copy to clipboard">
        <CopyToClipboardButton value={value} type="primary" />
      </Tooltip>
    </Input.Group>
  )
}

export default CopyableInput
