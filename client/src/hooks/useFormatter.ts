import React, { useContext } from 'react'

export const FormatterContext = React.createContext<{ locale: string }>({
  locale: 'de-DE'
})

const toDate = (date: Date | string) =>
  date instanceof Date ? date : new Date(date)

export const useFormatter = () => {
  const { locale } = useContext(FormatterContext)
  return {
    date: (date: Date | string) => toDate(date).toLocaleDateString(locale),
    dateTime: (date: Date | string) => toDate(date).toLocaleString(locale)
  }
}
