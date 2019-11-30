import { Spin } from 'antd'
import React, { useEffect, useState } from 'react'
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom'
import './App.less'
import Centered from './components/Centered'
import DefaultLayout from './components/DefaultLayout'
import { AuthenticatedUserContext } from './hooks/useAuthenticatedUser'
import AdminPage from './pages/AdminPage'
import AssignmentListPage from './pages/assignment/AssignmentListPage'
import AssignmentPage from './pages/assignment/AssignmentPage'
import LoginPage from './pages/LoginPage'
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
    return <LoginPage setAuthenticatedUser={setAuthenticatedUser} />
  }

  return (
    <AuthenticatedUserContext.Provider value={authenticatedUser}>
      <Router>
        <DefaultLayout>
          <Switch>
            <Route exact path="/assignments">
              <AssignmentListPage />
            </Route>
            <Route path="/assignments/:id">
              <AssignmentPage />
            </Route>
            <Route path="/admin">
              <AdminPage />
            </Route>
          </Switch>
        </DefaultLayout>
      </Router>
    </AuthenticatedUserContext.Provider>
  )
}

export default App
