import React, { useState } from 'react'
import Centered from './Centered'
import LaunchIdeSteps from './LaunchIdeSteps'
import { IdeType } from '../generated/graphql'
import { Col, Row } from 'antd'
import VersionSaveComponent from './VersionSaveComponent'

const IdeIframe: React.FC<{ type: IdeType; id: string }> = ({ type, id }) => {
  const [ideUrl, setIdeUrl] = useState<string | undefined>()

  return ideUrl ? (
    <Row gutter={[0, 28]}>
      <Col span={24}>
        <VersionSaveComponent answerId={id} />
      </Col>
      <Col span={24}>
        <iframe
          key={`ide-iframe-${id}`}
          className="ide-iframe"
          src={ideUrl}
          title="Edit answer"
        />
      </Col>
    </Row>
  ) : (
    <Centered>
      <LaunchIdeSteps type={type} id={id} onReady={setIdeUrl} />
    </Centered>
  )
}

export default IdeIframe
