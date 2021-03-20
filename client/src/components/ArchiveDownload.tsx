import { DownloadOutlined } from '@ant-design/icons'
import { Button, Dropdown, Menu } from 'antd'
import React from 'react'

const ArchiveDownload: React.FC<{ url: string }> = props => {
  const { url, children } = props
  const menu = (
    <Menu>
      <Menu.Item>
        <a href={url + '.zip'}>zip-Archive (.zip)</a>
      </Menu.Item>
      <Menu.Item>
        <a href={url + '.tar'}>tar-Archive (.tar)</a>
      </Menu.Item>
    </Menu>
  )
  return (
    <Dropdown overlay={menu}>
      <Button icon={<DownloadOutlined />}>{children}</Button>
    </Dropdown>
  )
}

export default ArchiveDownload
