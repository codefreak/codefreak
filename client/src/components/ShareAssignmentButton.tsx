import React, { useState } from 'react'
import { Popover, Button, Checkbox, Typography } from 'antd'
import { extractTargetChecked } from '../services/util'
import { HIDE_NAVIGATION_QUERY_PARAM } from '../hooks/useHideNavigation'

const { Paragraph } = Typography

export const ShareAssignmentButton: React.FC = () => {
  const [hideNavigation, setHideNavigation] = useState(false)
  const url =
    window.location.href +
    (hideNavigation ? `?${HIDE_NAVIGATION_QUERY_PARAM}=true` : '')
  return (
    <Popover
      content={
        <Typography>
          <Paragraph>
            <Checkbox onChange={extractTargetChecked(setHideNavigation)}>
              Single assignment view (hide navigation)
            </Checkbox>
          </Paragraph>
          <Paragraph copyable ellipsis underline style={{ maxWidth: 300 }}>
            {url}
          </Paragraph>
        </Typography>
      }
      title="Share Assignment"
      trigger="click"
      placement="bottom"
    >
      <Button icon="share-alt">Share</Button>
    </Popover>
  )
}
