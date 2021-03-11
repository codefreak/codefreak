import AsyncPlaceholder from '../../components/AsyncContainer'
import { useGetScoreboardByAssignmentIdQuery } from '../../generated/graphql'
import useIdParam from '../../hooks/useIdParam'
import ScoreboardTable from '../../components/autograder/ScoreboardTable'
import React from 'react'

const Scoreboard: React.FC = () => {
  const assignmentId = useIdParam()
  const result = ScoreboardByAssignmentHook(assignmentId)
  const fetchScoreboard = () => {
    return result.refetch() as any
  }

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  return (
    <ScoreboardTable
      scoreboardByAssignmentId={result.data.scoreboardByAssignmentId}
      fetchScoreboard={fetchScoreboard}
    />
  )
}

const ScoreboardByAssignmentHook = (assignmentId: string) => {
  const result = useGetScoreboardByAssignmentIdQuery({
    variables: { id: assignmentId }
  })

  return result
}

export default Scoreboard
