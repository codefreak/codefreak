import React from 'react'
import Helmet from 'react-helmet'
import { appName } from './DefaultLayout'

const SetTitle: React.FC = props => (
  <Helmet>
    <title>
      {props.children} - {appName}
    </title>
  </Helmet>
)

export default SetTitle
