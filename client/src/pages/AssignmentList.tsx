import React from 'react'
import useAuthenticatedUser from '../hooks/useAuthenticatedUser'

const AssignmentList: React.FC = () => {
  const user = useAuthenticatedUser()

  return <div>Hello {user.username}</div>
}

export default AssignmentList
