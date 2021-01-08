import { Alert, Card, Steps } from 'antd'
import React, { useEffect, useState } from 'react'
import {
  IdeType,
  useIsIdeLiveLazyQuery,
  useStartIdeMutation
} from '../generated/graphql'
import { extractErrorMessage } from '../services/codefreak-api'
import {
  CloudOutlined,
  ForwardOutlined,
  LoadingOutlined,
  RocketTwoTone
} from '@ant-design/icons'

const { Step } = Steps

const LaunchIdeSteps: React.FC<{
  type: IdeType
  id: string
  onReady: (url: string) => void
}> = ({ onReady, type, id }) => {
  const [currentStep, setCurrentStep] = useState(0)
  const [ideUrl, setIdeUrl] = useState<string>()
  const [error, setError] = useState<string>()

  const [startIde] = useStartIdeMutation({
    variables: { type, id },
    onError: e => setError(extractErrorMessage(e))
  })
  const [checkIsIdeLive, { data, loading }] = useIsIdeLiveLazyQuery({
    variables: { type, id },
    fetchPolicy: 'network-only'
  })

  useEffect(() => {
    if (error || loading || !ideUrl) return
    if (data?.isIdeLive === true) {
      setCurrentStep(2)
      onReady(ideUrl)
    } else {
      window.setTimeout(() => {
        checkIsIdeLive()
      }, 1000)
    }
  }, [onReady, error, ideUrl, data, loading, checkIsIdeLive])

  useEffect(() => {
    startIde().then(res => {
      if (res && res.data) {
        setCurrentStep(1)
        setIdeUrl(res.data.startIde)
        checkIsIdeLive()
      }
    })
  }, [startIde, checkIsIdeLive])

  const icon = (iconType: JSX.Element, step: number) =>
    step === currentStep && !error ? <LoadingOutlined /> : iconType

  return (
    <Card style={{ width: '100%', maxWidth: 800 }}>
      {error ? (
        <Alert message={error} type="error" style={{ marginBottom: 16 }} />
      ) : null}
      <Steps current={currentStep} status={error ? 'error' : 'process'}>
        <Step title="Launch Container" icon={icon(<RocketTwoTone />, 0)} />
        <Step title="Wait for IDE Startup" icon={icon(<CloudOutlined />, 1)} />
        <Step title="Redirect" icon={icon(<ForwardOutlined />, 2)} />
      </Steps>
    </Card>
  )
}

export default LaunchIdeSteps
