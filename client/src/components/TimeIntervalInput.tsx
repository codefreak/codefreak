import { InputNumber } from 'antd'
import React, { useState } from 'react'
import { TimeComponents } from '../services/time'
import './TimeIntervalInput.less'

const renderTimeIntervalInput = (
  suffix: string,
  value: number,
  onChange: (value: number) => void
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
    />
  )
}

export interface TimeIntervalInputProps {
  defaultValue?: TimeComponents
  onChange?: (newComponents: TimeComponents) => void
}

const TimeIntervalInput: React.FC<TimeIntervalInputProps> = ({
  defaultValue,
  onChange
}) => {
  const [components, setComponents] = useState<TimeComponents>(
    defaultValue || { hours: 0, minutes: 0, seconds: 0 }
  )

  const createOnValueChange = (field: keyof TimeComponents) => (
    value: number
  ) => {
    const newComponents = { ...components, [field]: value }
    setComponents(newComponents)
    if (onChange) {
      onChange(newComponents)
    }
  }
  return (
    <div className="time-interval-input">
      {renderTimeIntervalInput(
        'h',
        components.hours,
        createOnValueChange('hours')
      )}
      {renderTimeIntervalInput(
        'm',
        components.minutes,
        createOnValueChange('minutes')
      )}
      {renderTimeIntervalInput(
        's',
        components.seconds,
        createOnValueChange('seconds')
      )}
    </div>
  )
}

export default TimeIntervalInput
