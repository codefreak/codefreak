import { MenuDataItem } from '@ant-design/pro-layout/lib/typings'
import { Spin } from 'antd'
import React, { useEffect, useState } from 'react'
import {
  BrowserRouter as Router,
  Redirect,
  Route,
  Switch,
  useLocation
} from 'react-router-dom'
import './App.less'
import Centered from './components/Centered'
import DefaultLayout from './components/DefaultLayout'
import {
  AuthenticatedUser,
  AuthenticatedUserContext
} from './hooks/useAuthenticatedUser'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import { routerConfig } from './router.config'
import {
  useGetAuthenticatedUserQuery,
  useLogoutMutation
} from './services/codefreak-api'
import { messageService } from './services/message'

const App: React.FC = () => {
  const [authenticatedUser, setAuthenticatedUser] = useState<
    AuthenticatedUser
  >()

  const { data: authResult, loading } = useGetAuthenticatedUserQuery({
    context: { disableGlobalErrorHandling: true }
  })

  const [logout, { data: logoutSucceeded }] = useLogoutMutation()

  useEffect(() => {
    if (authResult !== undefined) {
      setAuthenticatedUser(authResult.me)
    }
  }, [authResult])

  useEffect(() => {
    if (logoutSucceeded) {
      messageService.success('Successfully signed out. Goodbye 👋')
      setAuthenticatedUser(undefined)
    }
  }, [logoutSucceeded])

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

  const routes: MenuDataItem[] = []
  flattenRoutes(routerConfig.routes || [], routes)

  return (
    <AuthenticatedUserContext.Provider value={authenticatedUser}>
      <Router>
        <DefaultLayout logout={logout}>
          <Switch>
            <Route exact path="/" key="1">
              <Redirect to="/assignments" />
            </Route>
            {routes.map(renderRoute)}
            <Route key="2" component={NotFoundPage} />
          </Switch>
        </DefaultLayout>
      </Router>
    </AuthenticatedUserContext.Provider>
  )
}

const flattenRoutes = (items: MenuDataItem[], routes: MenuDataItem[]) => {
  for (const item of items) {
    const { children = [], ...itemWihtoutChildren } = item
    flattenRoutes(children, routes)
    routes.push(itemWihtoutChildren)
  }
}

const renderRoute = (item: MenuDataItem, index: number): React.ReactNode => {
  return <Route key={index} {...item} />
}

export default App
