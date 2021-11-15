import { Tabs } from 'antd'
import { useContext, useState } from 'react'
import EvaluationHistory from '../../components/EvaluationHistory'
import { DifferentUserContext } from '../task/TaskPage'
import LatestEvaluation from '../../components/LatestEvaluation'

const { TabPane } = Tabs

const EvaluationPage: React.FC<{
  answerId: string
}> = ({ answerId }) => {
  const [activeTab, setActiveTab] = useState('')
  const differentUser = useContext(DifferentUserContext)

  const onTabChange = (activeKey: string) => setActiveTab(activeKey)

  return (
    <>
      <Tabs defaultActiveKey={activeTab} onChange={onTabChange}>
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
