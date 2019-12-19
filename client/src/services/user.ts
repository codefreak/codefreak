import { AuthenticatedUser } from '../hooks/useAuthenticatedUser'

export const displayName = (user: AuthenticatedUser) => {
  if (user.firstName || user.lastName) {
    return [user.firstName, user.lastName].join(' ')
  } else {
    return user.username
  }
}
export const initials = (user: AuthenticatedUser) => {
  return displayName(user).replace(/(\w)\w+\s*/g, '$1')
}
