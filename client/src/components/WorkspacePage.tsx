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
  const [workspaceUrl, setWorkspaceUrl] = useState<string | undefined>()
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
    setWorkspaceUrl(startWorkspaceResult.data?.startWorkspace.baseUrl)
  }, [startWorkspaceResult.data])
  useEffect(() => {
    if (deleteWorkspaceResult.data) {
      setWorkspaceUrl(undefined)
    }
  }, [deleteWorkspaceResult.data])
  if (workspaceUrl) {
    return (
      <>
        <h1>{workspaceUrl}</h1>
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
