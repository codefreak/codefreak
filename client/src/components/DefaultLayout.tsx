import ProLayout from '@ant-design/pro-layout'
import { MenuDataItem, Route } from '@ant-design/pro-layout/lib/typings'
import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { routerConfig } from '../router.config'

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
  return <Link to={menuItemProps.path}>{defaultDom}</Link>
}

export default DefaultLayout
