import { Button, Card, Icon } from 'antd'
import { Redirect } from 'react-router-dom'
import Centered from '../../components/Centered'
import useIdParam from '../../hooks/useIdParam'
import { shorten } from '../../services/short-id'
import { useQueryParam } from '../../hooks/useQuery'
import { HIDE_NAVIGATION_QUERY_PARAM } from '../../hooks/useHideNavigation'

const isDisplayedInIframe = () => {
  try {
    return window.self !== window.top
  } catch (e) {
    return true
  }
}

/**
 * This is the page a users sees if he clicks a resource link from the
 * LTI provider.
 * Currently, we do not support iframe embedding. This would require
 * Content Security Policy headers on all pages, even if the user is not working
 * via LTI. CSP frame-ancestors headers are only set on pages prefixed with /lti.
 * Redirecting the user to other pages will cause the browser to show a warning
 * at the moment.
 */
const LaunchPage: React.FC = () => {
  const hideNavigation = useQueryParam(HIDE_NAVIGATION_QUERY_PARAM)
  const assignmentId = useIdParam()
  let assignmentUrl = `/assignments/${shorten(assignmentId)}`
  if (hideNavigation === 'true') {
    assignmentUrl += `?${HIDE_NAVIGATION_QUERY_PARAM}=true`
  }

  if (!isDisplayedInIframe()) {
    return <Redirect to={assignmentUrl} />
  }

  const openInNewWindow = () => window.open(assignmentUrl, '_blank')

  return (
    <Centered style={{ flexGrow: 1 }}>
      <Card
        extra={<Icon type="info-circle" />}
        title="Embedding is not allowed"
        actions={[
          <Button key="blank" type="link" onClick={openInNewWindow}>
            Open new window
          </Button>
        ]}
        style={{ maxWidth: 360 }}
      >
        <p>
          It looks like you are viewing Code FREAK embedded in another
          application. For security reasons this is not allowed.
        </p>
        <p>
          Please click the link below to open the assignment in a new window.
        </p>
      </Card>
    </Centered>
  )
}

export default LaunchPage
