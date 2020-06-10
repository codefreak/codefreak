import { Moment } from 'moment'
import { zeroPad } from 'react-countdown'

export const toRelTime = (
  hours: number,
  minutes: number,
  seconds: number,
  forceSeconds: boolean = false
): string => {
  if (hours + minutes + seconds <= 0) {
    return '0s'
  }

  let str = ''
  if (hours > 0) {
    str += `${hours}h `
  }
  if (minutes > 0) {
    str += `${minutes}m `
  }
  if (forceSeconds || seconds > 0) {
    str += ` ${zeroPad(seconds, 2)}s`
  }
  return str.trimEnd()
}

export const secondsToRelTime = (sec: number): string => {
  const hours = Math.floor(sec / 3600)
  const minutes = Math.floor((sec - hours * 3600) / 60)
  const seconds = sec - hours * 3600 - minutes * 60
  return toRelTime(hours, minutes, seconds)
}

export function momentToDate<T>(fn: (date: Date) => T) {
  return (m: Moment) => fn(m.toDate())
}
