import { InputNumber, Switch } from 'antd'
import { InputNumberProps } from 'antd/es/input-number'
import React, { useState } from 'react'
import { TimeComponents } from '../services/time'
import './TimeIntervalInput.less'

const renderTimeIntervalInput = (
  suffix: string,
  value: number,
  onChange: (value: number | string) => void,
  additionalProps: InputNumberProps = {}
) => {
  const onChangeDefinitely = (val: string | number | undefined) =>
    val !== undefined ? onChange(val) : undefined

  const parser = (val: string | undefined) => {
    if (!val) {
      return ''
    }
    return val.replace(/[^\d]+/, '')
  }
  const formatter = (val: string | number | undefined) => `${val}${suffix}`

  return (
    <InputNumber
      min={0}
      onChange={onChangeDefinitely}
      value={value}
      parser={parser}
      formatter={formatter}
      {...additionalProps}
    />
  )
}

export interface TimeIntervalInputProps {
  defaultValue?: TimeComponents
  placeholder?: TimeComponents
  onChange?: (newComponents: TimeComponents | undefined) => void
  nullable?: boolean
}

const TimeIntervalInput: React.FC<TimeIntervalInputProps> = ({
  defaultValue,
  placeholder,
  onChange,
  nullable
}) => {
  const [components, setComponents] = useState<TimeComponents>(
    defaultValue || { hours: 0, minutes: 0, seconds: 0 }
  )
  const [enabled, setEnabled] = useState<boolean>(
    !nullable || defaultValue !== undefined
  )

  const createOnValueChange = (field: keyof TimeComponents) => (
    value: number | string
  ) => {
    const newComponents = { ...components, [field]: value }
    setComponents(newComponents)
    if (onChange) {
      onChange(enabled ? newComponents : undefined)
    }
  }

  const onEnabledChange = (state: boolean) => {
    setEnabled(state)
    if (onChange) {
      onChange(state ? components : undefined)
    }
  }

  const hours = enabled
    ? components.hours
    : placeholder?.hours || components.hours
  const minutes = enabled
    ? components.minutes
    : placeholder?.minutes || components.minutes
  const seconds = enabled
    ? components.seconds
    : placeholder?.seconds || components.seconds

  return (
    <div className="time-interval-input">
      {nullable ? (
        <Switch defaultChecked={enabled} onChange={onEnabledChange} />
      ) : undefined}
      <div className="time-interval-input-numbers">
        {renderTimeIntervalInput('h', hours, createOnValueChange('hours'), {
          disabled: !enabled
        })}
        {renderTimeIntervalInput('m', minutes, createOnValueChange('minutes'), {
          disabled: !enabled
        })}
        {renderTimeIntervalInput('s', seconds, createOnValueChange('seconds'), {
          disabled: !enabled
        })}
      </div>
    </div>
  )
}

export default TimeIntervalInput
