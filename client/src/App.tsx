import React, { useState } from 'react'
import './App.less'
import Login from './pages/Login'
import { User } from './services/codefreak-api'

const App: React.FC = () => {
  const [authenticatedUser, setAuthenticatedUser] = useState<User>()

  if (authenticatedUser === undefined) {
    return <Login setAuthenticatedUser={setAuthenticatedUser} />
  }

  return <div>Hello {authenticatedUser.username}</div>
}

export default App
