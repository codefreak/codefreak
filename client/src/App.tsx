import { MenuDataItem } from '@ant-design/pro-layout/lib/typings'
import { Spin } from 'antd'
import React, { useEffect, useState } from 'react'
import {
  BrowserRouter as Router,
  Redirect,
  Route,
  RouteProps,
  Switch
} from 'react-router-dom'
import './App.less'
import Centered from './components/Centered'
import DefaultLayout from './components/DefaultLayout'
import { AuthenticatedUserContext } from './hooks/useAuthenticatedUser'
import AdminPage from './pages/AdminPage'
import AssignmentListPage from './pages/assignment/AssignmentListPage'
import AssignmentPage from './pages/assignment/AssignmentPage'
import CreateAssignmentPage from './pages/assignment/CreateAssignmentPage'
import LoginPage from './pages/LoginPage'
import { routerConfig } from './router.config'
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

  const routes: MenuDataItem[] = []
  flattenRoutes(routerConfig.routes || [], routes)

  return (
    <AuthenticatedUserContext.Provider value={authenticatedUser}>
      <Router>
        <DefaultLayout>
          <Switch>
            <Route exact path="/" key="1">
              <Redirect to="/assignments" />
            </Route>
            {routes.map(renderRoute)}
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
