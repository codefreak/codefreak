import React from 'react'
import { useParams } from 'react-router-dom'
import Centered from '../components/Centered'
import LaunchIdeSteps from '../components/LaunchIdeSteps'
import useIdParam from '../hooks/useIdParam'
import { IdeType } from '../generated/graphql'
import { Result } from 'antd'

const getIdeType = (value: string): IdeType | undefined => {
  switch (value) {
    case 'task':
      return IdeType.Task
    case 'answer':
      return IdeType.Answer
    default:
      return undefined
  }
}

const IdePage: React.FC = () => {
  const onIdeReady = (url: string) => {
    window.location.href = url
  }
  const id = useIdParam()
  const queryIdeType = useParams<{ type: string }>().type
  const ideType = getIdeType(queryIdeType)

  if (ideType === undefined) {
    return <Result status="error" title={`Unknown IDE type ${queryIdeType}`} />
  }

  return (
    <Centered className="background-carbon">
      <LaunchIdeSteps type={ideType} id={id} onReady={onIdeReady} />
    </Centered>
  )
}

export default IdePage
