import ProLayout from '@ant-design/pro-layout'
import { MenuDataItem } from '@ant-design/pro-layout/lib/typings'
import { Route } from 'antd/lib/breadcrumb/Breadcrumb'
import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { routerConfig } from '../router.config'
import Authorized, { Role } from './Authorized'

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

const DefaultLayout: React.FC = props => {
  useLocation() // somehow this is needed for 'active navigation item' to work correctly 🤔
  return (
    <ProLayout
      menuItemRender={menuItemRender}
      route={routerConfig}
      title={appName}
      disableContentMargin={false}
      itemRender={breadcrumbItemRender}
    >
      {props.children}
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
    <Authorized role={menuItemProps.authority as Role}>
      <Link to={menuItemProps.path}>{defaultDom}</Link>
    </Authorized>
  )
}

export default DefaultLayout
