import { Avatar, Dropdown, Icon, Menu } from 'antd'
import useAuthenticatedUser from '../../hooks/useAuthenticatedUser'
import React from 'react'
import './index.less'
import { displayName, initials } from '../../services/user'

interface RightHeaderProps {
  logout: () => void
}

const Index: React.FC<RightHeaderProps> = ({ logout }) => {
  const user = useAuthenticatedUser();

  const userMenu = (
    <Menu selectedKeys={[]}>
      <Menu.Item key="logout" onClick={logout}>
        <Icon type="logout"/> Logout
      </Menu.Item>
    </Menu>
  )

  return <div style={{ float: 'right', paddingRight: 14 }}>
    <Dropdown overlay={userMenu}>
      <div style={{ padding: '0 10px' }}>
        <Avatar size="small" style={{ verticalAlign: 'text-top', marginRight: 10 }}>
          {initials(user)}
        </Avatar>
        {displayName(user)}
      </div>
    </Dropdown>
  </div>
}

export default Index
