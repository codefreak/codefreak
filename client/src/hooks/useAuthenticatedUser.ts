import { createContext, useContext } from 'react'
import { User } from '../services/codefreak-api'

export type AuthenticatedUser = Pick<
  User,
  'id' | 'username' | 'authorities' | 'firstName' | 'lastName'
>

export const AuthenticatedUserContext = createContext<
  AuthenticatedUser | undefined
>(undefined)

const useAuthenticatedUser = () => {
  const authenticatedUser = useContext(AuthenticatedUserContext)
  if (authenticatedUser === undefined) {
    throw new Error('User is not authenticated')
  }
  return authenticatedUser
}

export default useAuthenticatedUser
