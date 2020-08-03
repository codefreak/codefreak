import { MenuDataItem } from '@ant-design/pro-layout/lib/typings'
import { useApolloClient } from '@apollo/client'
import { Spin } from 'antd'
import React, { useEffect, useState } from 'react'
import {
  BrowserRouter as Router,
  Redirect,
  Route,
  Switch
} from 'react-router-dom'
import './App.less'
import Centered from './components/Centered'
import DefaultLayout from './components/DefaultLayout'
import ScrollToHash from './components/ScrollToHash'
import {
  AuthenticatedUser,
  AuthenticatedUserContext
} from './hooks/useAuthenticatedUser'
import IdePage from './pages/IdePage'
import LoginPage from './pages/LoginPage'
import LtiPage from './pages/LtiPage'
import NotFoundPage from './pages/NotFoundPage'
import { routerConfig } from './router.config'
import {
  useGetAuthenticatedUserQuery,
  useLogoutMutation
} from './services/codefreak-api'
import { messageService } from './services/message'
import { displayName } from './services/user'
import { noop } from './services/util'

const App: React.FC<{ onUserChanged?: () => void }> = props => {
  const onUserChanged = props.onUserChanged || noop
  const [authenticatedUser, setAuthenticatedUser] = useState<
    AuthenticatedUser
  >()

  const { data: authResult, loading } = useGetAuthenticatedUserQuery({
    context: { disableGlobalErrorHandling: true },
    fetchPolicy: 'network-only'
  })

  const [logout, { data: logoutSucceeded }] = useLogoutMutation()
  const apolloClient = useApolloClient()

  useEffect(() => {
    if (authResult !== undefined) {
      setAuthenticatedUser(authResult.me)
    }
  }, [authResult])

  useEffect(() => {
    if (logoutSucceeded) {
      messageService.success('Successfully signed out. Goodbye 👋')
      setAuthenticatedUser(undefined)
      onUserChanged()
    }
  }, [logoutSucceeded, onUserChanged])

  // make sure to delete cached data after login/logout
  useEffect(() => {
    apolloClient.clearStore()
  }, [authenticatedUser, apolloClient])

  if (loading) {
    return (
      <Centered>
        <Spin size="large" />
      </Centered>
    )
  }

  if (authenticatedUser === undefined) {
    const onLogin = (user: AuthenticatedUser) => {
      messageService.success(`Welcome back, ${displayName(user)}!`)
      setAuthenticatedUser(user)
      onUserChanged()
    }
    return <LoginPage onSuccessfulLogin={onLogin} />
  }

  const routes: MenuDataItem[] = []
  flattenRoutes(routerConfig.routes || [], routes)

  return (
    <AuthenticatedUserContext.Provider value={authenticatedUser}>
      <Router>
        <ScrollToHash />
        <Switch>
          <Route exact path="/">
            <Redirect to="/assignments" />
          </Route>
          <Route path="/ide/:type/:id" component={IdePage} />
          <Route path="/lti" component={LtiPage} />
          {routes.map(renderRoute(logout))}
          <DefaultLayout logout={logout}>
            <Route component={NotFoundPage} />
          </DefaultLayout>
        </Switch>
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

const renderRoute = (logout: () => {}) => (
  item: MenuDataItem,
  index: number
): React.ReactNode => {
  const { component: Component, ...props } = item
  return (
    <Route key={index} {...props}>
      <DefaultLayout logout={logout}>
        <Component />
      </DefaultLayout>
    </Route>
  )
}

export default App
