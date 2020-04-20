import short from 'short-uuid'

const translator = short()

const UUID_REGEX = /^[0-9A-F]{8}-[0-9A-F]{4}-[12345][0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12}$/i
export const isUUID = (id: string) => UUID_REGEX.test(id)

export const shorten = (id: string) => translator.fromUUID(id)

export const unshorten = (id: string) =>
  isUUID(id) ? id : translator.toUUID(id)
