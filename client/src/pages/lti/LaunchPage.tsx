import { Button, Card, Icon } from 'antd'
import React from 'react'
import { Redirect } from 'react-router-dom'
import Centered from '../../components/Centered'
import useIdParam from '../../hooks/useIdParam'
import { shorten } from '../../services/short-id'

const isDisplayedInIframe = () => {
  try {
    return window.self !== window.top
  } catch (e) {
    return true
  }
}

const LaunchPage: React.FC = () => {
  const assignmentId = useIdParam()
  const assignmentUrl = `/assignments/${shorten(assignmentId)}`

  if (!isDisplayedInIframe()) {
    return <Redirect to={assignmentUrl} />
  }

  const createContinue = (type: 'blank' | 'embedded') => () => {
    if (type === 'blank') {
      window.open(assignmentUrl, '_blank')
    } else {
      window.location.href = assignmentUrl
    }
  }

  return (
    <Centered style={{ flexGrow: 1 }}>
      <Card
        extra={<Icon type="info-circle" />}
        title={<>Open in new window?</>}
        actions={[
          <Button
            key="continue"
            type="link"
            style={{ color: 'grey' }}
            onClick={createContinue('embedded')}
          >
            Continue embedded
          </Button>,
          <Button key="blank" type="link" onClick={createContinue('blank')}>
            Open new window
          </Button>
        ]}
        style={{ maxWidth: 360 }}
      >
        <p>
          It looks like you are viewing Code FREAK embedded in another
          application. For the best experience we recommend opening it in a new
          window/tab.
        </p>
      </Card>
    </Centered>
  )
}

export default LaunchPage
