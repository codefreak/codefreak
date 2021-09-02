import TabPanel from './TabPanel'
import useWorkspace from '../../hooks/useWorkspace'
import { useEffect, useState } from 'react'
import { renderTaskInstructionsText } from '../../pages/task/TaskDetailsPage'

type InstructionsTabPanelProps = {
  loading: boolean
  baseUrl: string
}

const InstructionsTabPanel = ({
  loading,
  baseUrl
}: InstructionsTabPanelProps) => {
  // TODO reload automatically
  const { getFile } = useWorkspace(baseUrl)
  const [readme, setReadme] = useState<string | undefined>(undefined)

  useEffect(() => {
    if (!readme) {
      getFile('/README.md')
        .then(value => setReadme(value))
        .catch(error => console.error(error))
    }
  }, [getFile, readme])

  return (
    <TabPanel withPadding loading={loading}>
      {renderTaskInstructionsText(readme)}
    </TabPanel>
  )
}

export default InstructionsTabPanel
