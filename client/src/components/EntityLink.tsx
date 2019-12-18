import React from 'react'
import { Link } from 'react-router-dom'
import { Entity, getEntityPath } from '../services/entity-path'

interface EntityLinkProps {
  to: Entity
  sub?: string
}

const EntityLink: React.FC<EntityLinkProps> = props => {
  return (
    <Link to={getEntityPath(props.to) + (props.sub || '')}>
      {props.children}
    </Link>
  )
}

export default EntityLink
