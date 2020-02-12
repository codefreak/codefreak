import React, { useState } from 'react'
import Centered from './Centered'
import LaunchIdeSteps from './LaunchIdeSteps'

const IdeIframe: React.FC<{ type: string; id: string }> = ({ type, id }) => {
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
