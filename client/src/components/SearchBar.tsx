import React from 'react'
import {Input} from 'antd'

interface SearchBarProps {
  searchType: string
  placeholder: string
  onChange: (filterCriteria: string) => void
}

const SearchBar = (props: SearchBarProps) => {
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    props.onChange(e.target.value)
  }

  return (
    <Input.Search
      addonBefore={`Search ${props.searchType}`}
      placeholder={props.placeholder}
      allowClear
      onChange={handleChange}
    />
  )
}

export default SearchBar
