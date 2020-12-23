import React, { useState } from 'react'
import Centered from './Centered'
import LaunchIdeSteps from './LaunchIdeSteps'
import { IdeType } from '../generated/graphql'

const IdeIframe: React.FC<{ type: IdeType; id: string }> = ({ type, id }) => {
  const [ideUrl, setIdeUrl] = useState<string | undefined>()

  return ideUrl ? (
    <iframe
      key={`ide-iframe-${id}`}
      className="ide-iframe"
      src={ideUrl}
      title="Edit answer"
    />
  ) : (
    <Centered>
      <LaunchIdeSteps type={type} id={id} onReady={setIdeUrl} />
    </Centered>
  )
}

export default IdeIframe
