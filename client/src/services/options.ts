import { useState } from 'react'
import createPersistedState from 'use-persisted-state'

export const createOptionState = (key: string): typeof useState => {
  return createPersistedState(key)
}
