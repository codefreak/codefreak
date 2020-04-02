import ProLayout from '@ant-design/pro-layout'
import { MenuDataItem } from '@ant-design/pro-layout/lib/typings'
import { Alert } from 'antd'
import { Route } from 'antd/lib/breadcrumb/Breadcrumb'
import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Authority } from '../hooks/useHasAuthority'
import useSubscribeToGlobalEvents from '../hooks/useSubscribeToGlobalEvents'
import { routerConfig } from '../router.config'
import { useGetMotdQuery } from '../services/codefreak-api'
import AppFooter from './AppFooter'
import Authorized from './Authorized'
import './DefaultLayout.less'
import Logo from './Logo'
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

  useSubscribeToGlobalEvents()

  const motd = useGetMotdQuery()

  const renderRightHeader = () => <RightHeader logout={logout} />
  const renderFooter = () => <AppFooter />
  const renderHeader = (_: any, defaultDom: React.ReactNode) => (
    <>
      {defaultDom}
      {motd.data && motd.data.motd ? (
        <Alert banner message={motd.data.motd} />
      ) : null}
    </>
  )

  return (
    <ProLayout
      menuItemRender={menuItemRender}
      route={routerConfig}
      title={appName}
      logo={<Logo />}
      disableContentMargin={false}
      itemRender={breadcrumbItemRender}
      rightContentRender={renderRightHeader}
      footerRender={renderFooter}
      headerRender={renderHeader}
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
