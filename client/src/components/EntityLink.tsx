import React from 'react'
import { Link } from 'react-router-dom'
import { Entity, getEntityPath } from '../services/entity-path'

const EntityLink: React.FC<{ to: Entity }> = props => {
  return <Link to={getEntityPath(props.to)}>{props.children}</Link>
}

export default EntityLink
