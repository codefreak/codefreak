import React, { useEffect, useState } from 'react'
import {
  FileContextType,
  useDeleteWorkspaceMutation,
  useStartWorkspaceMutation
} from '../generated/graphql'
import { Button } from 'antd'
import { DeleteFilled, RocketFilled } from '@ant-design/icons'

export interface WorkspacePageProps {
  id: string
  type: FileContextType
}

const WorkspacePage: React.FC<WorkspacePageProps> = props => {
  const [workspaceInfo, setWorkspaceInfo] = useState<{baseUrl: string, authToken: string | null} | undefined>()
  const variables = {
    context: {
      id: props.id,
      type: props.type
    }
  }
  const [startWorkspace, startWorkspaceResult] = useStartWorkspaceMutation({
    variables
  })
  const [deleteWorkspace, deleteWorkspaceResult] = useDeleteWorkspaceMutation({
    variables
  })
  useEffect(() => {
    setWorkspaceInfo(startWorkspaceResult.data?.startWorkspace)
  }, [startWorkspaceResult.data])
  useEffect(() => {
    if (deleteWorkspaceResult.data) {
      setWorkspaceInfo(undefined)
    }
  }, [deleteWorkspaceResult.data])
  if (workspaceInfo) {
    return (
      <>
        <h1>{workspaceInfo.baseUrl}</h1>
        <pre>
          <code>{workspaceInfo.authToken}</code>
        </pre>
        <Button
          danger
          icon={<DeleteFilled />}
          onClick={() => deleteWorkspace()}
        >
          Stop Workspace
        </Button>
      </>
    )
  }

  return (
    <Button
      type="primary"
      icon={<RocketFilled />}
      loading={startWorkspaceResult.loading}
      onClick={() => startWorkspace()}
    >
      Start Workspace
    </Button>
  )
}

export default WorkspacePage
