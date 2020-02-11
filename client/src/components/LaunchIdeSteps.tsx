import { Alert, Card, Icon, Steps } from 'antd'
import React, { useCallback, useEffect, useState } from 'react'
import { useStartIdeMutation } from '../generated/graphql'
import { extractErrorMessage } from '../services/codefreak-api'

const { Step } = Steps

const LaunchIdeSteps: React.FC<{
  type: string
  id: string
  onReady: (url: string) => void
}> = ({ onReady, type, id }) => {
  const [currentStep, setCurrentStep] = useState(0)
  const [error, setError] = useState<string>()

  const [startIde] = useStartIdeMutation({
    variables: { type, id },
    onError: e => setError(extractErrorMessage(e))
  })

  const checkIde = useCallback(async (url: string) => {
    try {
      const res = await fetch(url)
      if (res.ok) {
        onReady(url)
      } else {
        throw new Error()
      }
    } catch (e) {
      setTimeout(checkIde.bind(null, url), 1000)
    }
  }, [])

  useEffect(() => {
    startIde().then(res => {
      if (res && res.data) {
        setCurrentStep(c => c + 1)
        checkIde(res.data.startIde)
      }
    })
  }, [startIde, checkIde])

  const icon = (type: string, step: number) => (
    <Icon type={step === currentStep && !error ? 'loading' : type} />
  )

  return (
    <Card style={{ width: '100%', maxWidth: 800 }}>
      {error ? (
        <Alert message={error} type="error" style={{ marginBottom: 16 }} />
      ) : null}
      <Steps current={currentStep} status={error ? 'error' : 'process'}>
        <Step title="Launch Container" icon={icon('rocket', 0)} />
        <Step title="Wait for IDE Startup" icon={icon('cloud', 1)} />
        <Step title="Redirect" icon={icon('forward', 2)} />
      </Steps>
    </Card>
  )
}

export default LaunchIdeSteps
