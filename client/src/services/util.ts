import { Moment } from 'moment'

export const noop = () => {
  /* do nothing */
}

export function makeUpdater<T>(
  currentValue: T,
  callback: (newValue: T) => any
) {
  return <P extends keyof T>(propName: P) => (propValue: T[P]) => {
    const newValue = { ...currentValue }
    if (propValue === undefined) {
      delete newValue[propName]
    } else {
      newValue[propName] = propValue
    }
    callback(newValue)
  }
}

export function momentToDate<T>(fn: (date: Date) => T) {
  return (moment: Moment) => fn(moment.toDate())
}
