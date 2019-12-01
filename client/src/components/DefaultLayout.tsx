import ProLayout from '@ant-design/pro-layout'
import { MenuDataItem } from '@ant-design/pro-layout/lib/typings'
import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { routerConfig } from '../router.config'
import Authorized, { Role } from './Authorized'

const DefaultLayout: React.FC = props => {
  useLocation() // somehow this is needed for 'active navigation item' to work correctly 🤔
  return (
    <ProLayout
      menuItemRender={menuItemRender}
      route={routerConfig}
      title="Code FREAK"
      disableContentMargin={false}
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
