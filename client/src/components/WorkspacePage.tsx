import React from 'react'
import {
  FileContextType,
  useStartWorkspaceMutation
} from '../generated/graphql'

export interface WorkspacePageProps {
  id: string
  type: FileContextType
}

const WorkspacePage: React.FC<WorkspacePageProps> = props => {
  const [startWorkspace, workspace] = useStartWorkspaceMutation({
    variables: {
      context: {
        id: props.id,
        type: props.type
      }
    }
  })
  return (
    <h1>
      Moin {workspace.data?.startWorkspace.baseUrl}{' '}
      {!workspace.data ? (
        <button onClick={() => startWorkspace()}>Start</button>
      ) : null}
    </h1>
  )
}

export default WorkspacePage
