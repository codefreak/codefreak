import React from 'react'
import useAuthenticatedUser from '../hooks/useAuthenticatedUser'

export const ROLES = {
  ADMIN: 'ADMIN',
  TEACHER: 'TEACHER',
  STUDENT: 'STUDENT'
}

export type Role = keyof typeof ROLES

interface AuthorizedProps {
  role?: Role
  condition?: boolean
}

const Authorized: React.FC<AuthorizedProps> = ({
  role,
  condition,
  children
}) => {
  const user = useAuthenticatedUser()

  if (role !== undefined && !user.roles.includes(role)) {
    return null
  }

  if (condition !== undefined && !condition) {
    return null
  }

  return <>{children}</>
}

export default Authorized
