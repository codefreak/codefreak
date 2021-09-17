import TabPanel from './TabPanel'
import EvaluationPage from '../../pages/evaluation/EvaluationOverviewPage'
import useWorkspace from '../../hooks/workspace/useWorkspace'

const EvaluationTabPanel = () => {
  const { answerId } = useWorkspace()
  return (
    <TabPanel withPadding>
      <EvaluationPage answerId={answerId} />
    </TabPanel>
  )
}

export default EvaluationTabPanel
