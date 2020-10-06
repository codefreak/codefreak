import {Select} from 'antd'
import React from 'react'
import {capitalize} from "../services/strings";

interface SortSelectProps {
  defaultValue: string
  values: string[]
  onSortChange: (value: string) => void
}

const SortSelect = (props: SortSelectProps) => {
  const options = props.values.map(
    optionValue =>
      (
        <Select.Option value={optionValue}>
          {`Sort by ${capitalize(optionValue)}`}
        </Select.Option>
      )
  )

  return (
    <Select
      defaultValue={props.defaultValue}
      onChange={props.onSortChange}
      style={{width: '150px'}}
    >
      {options}
    </Select>
  )
}

export default SortSelect
