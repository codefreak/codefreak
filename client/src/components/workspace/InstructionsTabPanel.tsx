import TabPanel from './TabPanel'
import { renderTaskInstructionsText } from '../../pages/task/TaskDetailsPage'
import useGetWorkspaceFileQuery from '../../hooks/workspace/useGetWorkspaceFileQuery'

type InstructionsTabPanelProps = {
  loading: boolean
}

const InstructionsTabPanel = ({ loading }: InstructionsTabPanelProps) => {
  const { data: readme, isLoading: isLoadingFile } =
    useGetWorkspaceFileQuery('/README.md')

  return (
    <TabPanel withPadding loading={loading || isLoadingFile}>
      {renderTaskInstructionsText(readme)}
    </TabPanel>
  )
}

export default InstructionsTabPanel
