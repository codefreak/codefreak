import { Tabs } from 'antd'
import React, { useContext } from 'react'
import EvaluationHistory from '../../components/EvaluationHistory'
import useSubPath from '../../hooks/useSubPath'
import { shorten } from '../../services/short-id'
import { DifferentUserContext } from '../task/TaskPage'
import LatestEvaluation from '../../components/LatestEvaluation'
import EvaluationStatusTracker from '../../components/EvaluationStatusTracker'

const { TabPane } = Tabs

const EvaluationPage: React.FC<{
  answerId: string
}> = ({ answerId }) => {
  const subPath = useSubPath()
  const differentUser = useContext(DifferentUserContext)

  const onTabChange = (activeKey: string) => {
    subPath.set(
      activeKey,
      differentUser ? { user: shorten(differentUser.id) } : undefined
    )
  }

  return (
    <>
      {!differentUser ? (
        <div style={{ padding: '0 64px', marginBottom: 16 }}>
          <EvaluationStatusTracker answerId={answerId} />
        </div>
      ) : null}
      <Tabs defaultActiveKey={subPath.get()} onChange={onTabChange}>
        <TabPane tab="Latest Evaluation" key="">
          <LatestEvaluation answerId={answerId} showTrigger={!differentUser} />
        </TabPane>
        <TabPane tab="Evaluation History" key="/history">
          <EvaluationHistory answerId={answerId} />
        </TabPane>
      </Tabs>
    </>
  )
}

export default EvaluationPage
