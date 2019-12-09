import ProLayout from '@ant-design/pro-layout'
import { MenuDataItem } from '@ant-design/pro-layout/lib/typings'
import { Button, Tooltip } from 'antd'
import { Route } from 'antd/lib/breadcrumb/Breadcrumb'
import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Authority } from '../hooks/useHasAuthority'
import { routerConfig } from '../router.config'
import Authorized from './Authorized'

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

const DefaultLayout: React.FC<DefaultLayoutProps> = props => {
  useLocation() // somehow this is needed for 'active navigation item' to work correctly 🤔

  const renderHeader = () => (
    <span style={{ float: 'right' }}>
      <Tooltip title="Sign out" placement="left">
        <Button
          onClick={props.logout}
          icon="logout"
          shape="circle"
          style={{ marginRight: 16 }}
        />
      </Tooltip>
    </span>
  )

  return (
    <ProLayout
      menuItemRender={menuItemRender}
      route={routerConfig}
      title={appName}
      logo={process.env.PUBLIC_URL + '/codefreak-logo.svg'}
      disableContentMargin={false}
      itemRender={breadcrumbItemRender}
      rightContentRender={renderHeader}
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
    <Authorized authority={menuItemProps.authority as Authority}>
      <Link to={menuItemProps.path}>{defaultDom}</Link>
    </Authorized>
  )
}

export default DefaultLayout
