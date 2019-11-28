import React, { useState } from 'react'
import './App.less'
import { AuthenticatedUserContext } from './hooks/useAuthenticatedUser'
import AssignmentList from './pages/AssignmentList'
import Login from './pages/Login'
import { User } from './services/codefreak-api'

const App: React.FC = () => {
  const [authenticatedUser, setAuthenticatedUser] = useState<User>()

  if (authenticatedUser === undefined) {
    return <Login setAuthenticatedUser={setAuthenticatedUser} />
  }

  return (
    <AuthenticatedUserContext.Provider value={authenticatedUser}>
      <AssignmentList />
    </AuthenticatedUserContext.Provider>
  )
}

export default App
