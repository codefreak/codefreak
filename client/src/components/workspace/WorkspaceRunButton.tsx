import { Button } from 'antd'
import { CaretRightOutlined } from '@ant-design/icons'
import useStartProcessMutation from '../../hooks/workspace/useStartProcessMutation'
import { useEffect } from 'react'
import { useMutableQueryParam } from '../../hooks/useQuery'
import { WorkspaceTabType } from '../../services/workspace-tabs'
import { RIGHT_TAB_QUERY_PARAM } from './WorkspacePage'

interface WorkspaceRunButtonProps {
  onRunProcessStarted: (runProcessId: string) => void
}

const WorkspaceRunButton = ({
  onRunProcessStarted
}: WorkspaceRunButtonProps) => {
  const {
    mutate: run,
    isLoading,
    data
  } = useStartProcessMutation(['bash', '-c', 'python main.py'])
  const [activeRightTab, setActiveRightTab] = useMutableQueryParam(
    RIGHT_TAB_QUERY_PARAM,
    ''
  )

  useEffect(() => {
    if (!isLoading && data) {
      onRunProcessStarted(data)
    }
  }, [isLoading, data, onRunProcessStarted])

  const handleClick = () => {
    if (!isLoading) {
      run()

      if (activeRightTab !== WorkspaceTabType.CONSOLE) {
        setActiveRightTab(WorkspaceTabType.CONSOLE)
      }
    }
  }

  return (
    <Button
      icon={<CaretRightOutlined />}
      type="primary"
      size="large"
      shape="round"
      loading={isLoading}
      onClick={handleClick}
    >
      Run
    </Button>
  )
}

export default WorkspaceRunButton