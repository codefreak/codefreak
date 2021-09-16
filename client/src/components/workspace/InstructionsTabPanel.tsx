import TabPanel from './TabPanel'
import { renderTaskInstructionsText } from '../../pages/task/TaskDetailsPage'
import { useGetTaskDetailsQuery } from '../../generated/graphql'
import useIdParam from '../../hooks/useIdParam'

const InstructionsTabPanel = () => {
  const { data } = useGetTaskDetailsQuery({
    variables: { id: useIdParam(), teacher: false }
  })
  const instructions = data?.task.body ?? ''
  // TODO use the whole panel from TaskDetailsPage (?)
  // TODO max height for instructions (?)

  return (
    <TabPanel withPadding>{renderTaskInstructionsText(instructions)}</TabPanel>
  )
}

export default InstructionsTabPanel
