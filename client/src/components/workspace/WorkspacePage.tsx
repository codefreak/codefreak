import {
  FileContextType,
  useStartWorkspaceMutation
} from '../../generated/graphql'
import './WorkspacePage.less'
import WorkspaceTabsWrapper from './WorkspaceTabsWrapper'
import { useEffect, useState } from 'react'

export interface WorkspacePageProps {
  id: string
  type: FileContextType
}

const WorkspacePage = ({ id, type }: WorkspacePageProps) => {
  const [startWorkspace, { data, called }] = useStartWorkspaceMutation({
    variables: {
      context: {
        id,
        type
      }
    }
  })
  const [baseUrl, setBaseUrl] = useState('')

  useEffect(() => {
    if (!data && !called) {
      startWorkspace()
    }
  })

  useEffect(() => {
    if (data && baseUrl.length === 0) {
      setBaseUrl(data.startWorkspace.baseUrl)
    }
  }, [data, baseUrl])

  return <WorkspaceTabsWrapper baseUrl={baseUrl} />
}

export default WorkspacePage
