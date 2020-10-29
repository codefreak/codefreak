import { InputNumber, Switch } from 'antd'
import { InputNumberProps } from 'antd/es/input-number'
import React, { useState } from 'react'
import { TimeComponents } from '../services/time'
import './TimeIntervalInput.less'

const renderTimeIntervalInput = (
  suffix: string,
  value: number,
  onChange: (value: number) => void,
  additionalProps: InputNumberProps = {}
) => {
  const onChangeDefinitely = (val: number | undefined) =>
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
  defaultValue?: TimeComponents | undefined
  onChange?: (newComponents: TimeComponents | undefined) => void
  nullable?: boolean
}

const TimeIntervalInput: React.FC<TimeIntervalInputProps> = ({
  defaultValue,
  onChange,
  nullable
}) => {
  const [components, setComponents] = useState<TimeComponents>(
    defaultValue || { hours: 0, minutes: 0, seconds: 0 }
  )
  const [enabled, setEnabled] = useState<boolean>(!nullable || !!defaultValue)

  const createOnValueChange = (field: keyof TimeComponents) => (
    value: number
  ) => {
    const newComponents = { ...components, [field]: value }
    setComponents(newComponents)
    if (onChange) {
      onChange(newComponents)
    }
  }

  const onEnabledChange = (state: boolean) => {
    setEnabled(state)
    if (onChange) {
      onChange(state ? components : undefined)
    }
  }

  return (
    <div className="time-interval-input">
      {nullable ? (
        <Switch defaultChecked={enabled} onChange={onEnabledChange} />
      ) : undefined}
      <div className="time-interval-input-numbers">
        {renderTimeIntervalInput(
          'h',
          components.hours,
          createOnValueChange('hours'),
          { disabled: !enabled }
        )}
        {renderTimeIntervalInput(
          'm',
          components.minutes,
          createOnValueChange('minutes'),
          { disabled: !enabled }
        )}
        {renderTimeIntervalInput(
          's',
          components.seconds,
          createOnValueChange('seconds'),
          { disabled: !enabled }
        )}
      </div>
    </div>
  )
}

export default TimeIntervalInput
