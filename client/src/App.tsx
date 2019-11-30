import { Spin } from 'antd'
import React, { useEffect, useState } from 'react'
import './App.less'
import Centered from './components/Centered'
import { AuthenticatedUserContext } from './hooks/useAuthenticatedUser'
import AssignmentList from './pages/AssignmentList'
import Login from './pages/Login'
import { useGetAuthenticatedUserQuery, User } from './services/codefreak-api'

const App: React.FC = () => {
  const [authenticatedUser, setAuthenticatedUser] = useState<User>()

  const { data, loading } = useGetAuthenticatedUserQuery({
    context: { disableGlobalErrorHandling: true }
  })

  useEffect(() => {
    if (data !== undefined) {
      setAuthenticatedUser(data.me)
    }
  }, [data])

  if (loading) {
    return (
      <Centered>
        <Spin size="large" />
      </Centered>
    )
  }

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
