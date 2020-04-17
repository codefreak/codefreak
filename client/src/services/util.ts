import { Moment } from 'moment'

export const noop = () => {
  /* do nothing */
}

export function makeUpdater<T, R>(
  currentValue: T,
  callback: (newValue: T) => Promise<R>
): Updater<T, R> {
  return <P extends keyof T>(propName: P) => (propValue: T[P]) => {
    const newValue = { ...currentValue }
    if (propValue === undefined) {
      delete newValue[propName]
    } else {
      newValue[propName] = propValue
    }
    return callback(newValue)
  }
}

export type Updater<T, R = any> = <P extends keyof T>(
  propName: P
) => (propValue: T[P]) => Promise<R>

export function momentToDate<T>(fn: (date: Date) => T) {
  return (moment: Moment) => fn(moment.toDate())
}

export function extractTargetValue<V, T>(fn: (value: V) => T) {
  return (e: { target: { value: V } }) => fn(e.target.value)
}
