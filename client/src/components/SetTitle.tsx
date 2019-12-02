import React from 'react'
import Helmet from 'react-helmet'

const SetTitle: React.FC = props => (
  <Helmet>
    <title>{props.children} - Code FREAK</title>
  </Helmet>
)

export default SetTitle
