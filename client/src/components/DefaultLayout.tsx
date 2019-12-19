import ProLayout from '@ant-design/pro-layout'
import { MenuDataItem } from '@ant-design/pro-layout/lib/typings'
import { Route } from 'antd/lib/breadcrumb/Breadcrumb'
import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Authority } from '../hooks/useHasAuthority'
import { routerConfig } from '../router.config'
import Authorized from './Authorized'
import RightHeader from './RightHeader'

export const appName = 'Code FREAK'

export const breadcrumbItemRender = (route: Route, _: any, routes: Route[]) => {
  const last = routes.indexOf(route) === routes.length - 1
  return last ? (
    <span>{route.breadcrumbName}</span>
  ) : (
    <Link to={route.path}>{route.breadcrumbName}</Link>
  )
}

export const createBreadcrumb = (routes: Route[]) => ({
  routes,
  itemRender: breadcrumbItemRender
})

interface DefaultLayoutProps {
  logout: () => void
}

const DefaultLayout: React.FC<DefaultLayoutProps> = ({ logout, children }) => {
  useLocation() // somehow this is needed for 'active navigation item' to work correctly 🤔

  const renderRightHeader = () => <RightHeader logout={logout} />

  return (
    <ProLayout
      menuItemRender={menuItemRender}
      route={routerConfig}
      title={appName}
      logo={process.env.PUBLIC_URL + '/codefreak-logo.svg'}
      disableContentMargin={false}
      itemRender={breadcrumbItemRender}
      rightContentRender={renderRightHeader}
    >
      {children}
    </ProLayout>
  )
}

const menuItemRender = (
  menuItemProps: MenuDataItem,
  defaultDom: React.ReactNode
) => {
  if (menuItemProps.isUrl || menuItemProps.children) {
    return defaultDom
  }
  return (
    <Authorized authority={menuItemProps.authority as Authority}>
      <Link to={menuItemProps.path}>{defaultDom}</Link>
    </Authorized>
  )
}

export default DefaultLayout
