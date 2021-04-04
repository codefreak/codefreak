import AsyncPlaceholder from '../../components/AsyncContainer'
import { useGetScoreboardByAssignmentIdQuery } from '../../generated/graphql'
import useIdParam from '../../hooks/useIdParam'
import ScoreboardTable from '../../components/autograder/ScoreboardTable'
import React from 'react'

const Scoreboard: React.FC = () => {
  const assignmentId = useIdParam()
  const result = useGetScoreboardByAssignmentIdQuery({
    variables: { id: assignmentId }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  return (
    <ScoreboardTable
      scoreboardByAssignmentId={result.data.scoreboardByAssignmentId}
    />
  )
}

export default Scoreboard
