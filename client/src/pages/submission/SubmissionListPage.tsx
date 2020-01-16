import React from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import SubmissionsTable from '../../components/SubmissionsTable'
import { useGetAssignmentWithSubmissionsQuery } from '../../generated/graphql'
import useIdParam from '../../hooks/useIdParam'

const SubmissionListPage: React.FC = () => {
  const assignmentId = useIdParam()
  const result = useGetAssignmentWithSubmissionsQuery({
    variables: { id: assignmentId }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  return <SubmissionsTable assignment={result.data.assignment} />
}

export default SubmissionListPage
