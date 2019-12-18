import { shorten } from './short-id'

export const BASE_PATHS = {
  Assignment: '/assignments',
  Task: '/tasks'
}

type TypeName = keyof typeof BASE_PATHS

export interface Entity {
  __typename?: TypeName
  id: string
}

export const getEntityPath = (entity: Entity) => {
  if (entity.__typename === undefined) {
    throw new Error('__typename must be defined')
  }
  return BASE_PATHS[entity.__typename] + '/' + shorten(entity.id)
}
