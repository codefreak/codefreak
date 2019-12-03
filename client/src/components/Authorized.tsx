import React from 'react'
import useAuthenticatedUser from '../hooks/useAuthenticatedUser'
import { Authority } from '../hooks/useHasAuthority'

interface AuthorizedProps {
  authority?: Authority
  condition?: boolean
}

const Authorized: React.FC<AuthorizedProps> = ({
  authority,
  condition,
  children
}) => {
  const user = useAuthenticatedUser()

  if (authority !== undefined && !user.authorities.includes(authority)) {
    return null
  }

  if (condition !== undefined && !condition) {
    return null
  }

  return <>{children}</>
}

export default Authorized
