import { LogoutOutlined } from '@ant-design/icons'
import { Dropdown, Menu } from 'antd'
import React from 'react'
import useAuthenticatedUser from '../../hooks/useAuthenticatedUser'
import { displayName } from '../../services/user'
import Avatar from '../user/Avatar'
import './index.less'

interface RightHeaderProps {
  logout?: () => void
}

const Index: React.FC<RightHeaderProps> = ({ logout }) => {
  const user = useAuthenticatedUser()

  const userMenu = logout ? (
    <Menu selectedKeys={[]}>
      <Menu.Item key="logout" onClick={logout}>
        <LogoutOutlined /> Logout
      </Menu.Item>
    </Menu>
  ) : undefined

  return (
    <div style={{ float: 'right', paddingRight: 14 }}>
      <Dropdown
        overlay={userMenu ?? <Menu />}
        disabled={!userMenu}
        trigger={['click']}
      >
        <div style={{ padding: '0 10px' }}>
          <Avatar size="small" user={user} />
          {displayName(user)}
        </div>
      </Dropdown>
    </div>
  )
}

export default Index
