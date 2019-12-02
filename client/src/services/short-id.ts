import short from 'short-uuid'

const translator = short()

export const shorten = (id: string) => translator.fromUUID(id)

export const unshorten = (id: string) => translator.toUUID(id)
