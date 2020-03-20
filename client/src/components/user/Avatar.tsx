import { Avatar as AntdAvatar } from 'antd'
import { AvatarProps as AntdAvatarProps } from 'antd/lib/avatar'
import React from 'react'
import { PublicUserFieldsFragment } from '../../generated/graphql'
import { initials } from '../../services/user'

export interface AvatarProps extends AntdAvatarProps {
  user: Pick<PublicUserFieldsFragment, 'firstName' | 'lastName' | 'username'>
}

const Avatar: React.FC<AvatarProps> = props => {
  return (
    <AntdAvatar
      style={{ verticalAlign: 'text-top', marginRight: 10 }}
      {...props}
    >
      {initials(props.user)}
    </AntdAvatar>
  )
}

export default Avatar
