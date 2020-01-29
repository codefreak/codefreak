interface UserNameProperties {
  firstName?: string | null
  lastName?: string | null
  username: string
}

export const displayName = (user: UserNameProperties) => {
  if (user.firstName || user.lastName) {
    return [user.firstName, user.lastName].join(' ')
  } else {
    return user.username
  }
}
export const initials = (user: UserNameProperties) => {
  return displayName(user).replace(/(\w)\w+\s*/g, '$1')
}
