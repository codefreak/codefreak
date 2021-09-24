import TabPanel from './TabPanel'
import { renderTaskInstructionsText } from '../../pages/task/TaskDetailsPage'
import { useGetTaskDetailsQuery } from '../../generated/graphql'
import useWorkspace from '../../hooks/workspace/useWorkspace'

const InstructionsTabPanel = () => {
  const { taskId: id } = useWorkspace()
  const { data } = useGetTaskDetailsQuery({
    variables: { id, teacher: false }
  })
  const instructions = data?.task.body ?? ''

  return (
    <TabPanel withPadding>{renderTaskInstructionsText(instructions)}</TabPanel>
  )
}

export default InstructionsTabPanel
