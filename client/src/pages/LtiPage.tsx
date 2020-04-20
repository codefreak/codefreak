import React from 'react'
import { Route, Switch } from 'react-router-dom'
import EmbeddedLayout from '../components/EmbeddedLayout'
import DeepLinkPage from '../pages/lti/DeepLinkPage'
import LaunchPage from '../pages/lti/LaunchPage'
import NotFoundPage from './NotFoundPage'

const LtiPage: React.FC = () => {
  return (
    <EmbeddedLayout>
      <Switch>
        <Route path="/lti/deep-link" component={DeepLinkPage} />
        <Route path="/lti/launch/:id" component={LaunchPage} />
        <Route>
          <NotFoundPage />
        </Route>
      </Switch>
    </EmbeddedLayout>
  )
}

export default LtiPage
