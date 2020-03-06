import React from 'react'
import { useParams } from 'react-router'
import Centered from '../components/Centered'
import LaunchIdeSteps from '../components/LaunchIdeSteps'
import useIdParam from '../hooks/useIdParam'

const IdePage: React.FC = () => {
  const onIdeReady = (url: string) => {
    window.location.href = url
  }

  return (
    <Centered className="background-carbon">
      <LaunchIdeSteps
        type={useParams<{ type: string }>().type}
        id={useIdParam()}
        onReady={onIdeReady}
      />
    </Centered>
  )
}

export default IdePage
