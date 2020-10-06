import React from 'react'

interface SortedListProps<T> {
  list: T[]
  sort: (a: T, b: T) => number
  render: (a: T) => JSX.Element
}

const SortedList = <T extends unknown>(props: SortedListProps<T>) => (
  <>{props.list.slice().sort(props.sort).map(props.render)}</>
)

export default SortedList
