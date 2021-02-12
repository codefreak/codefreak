import AsyncPlaceholder from '../../components/AsyncContainer'
import {useGetScoreboardByAssignmentIdQuery} from '../../generated/graphql'
import useIdParam from '../../hooks/useIdParam'
import ScoreboardTable from "../../components/autograder/ScoreboardTable";
import React, {useEffect, useState} from "react";
// import ScoreboardTable from "../../components/ScoreboardTable";

const Scoreboard: React.FC = () => {
  const assignmentId = useIdParam()
  const result = ScoreboardByAssignmentHook(assignmentId)
  const fetchScoreboard=()=>{
    return result.refetch() as any
  }

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  // return (
  //   <div><p>you clicked {result.data.scoreboardByAssignmentId.id} id</p><button onClick={()=> undefined}>click</button></div>
  // )

  return <ScoreboardTable scoreboardByAssignmentId={result.data.scoreboardByAssignmentId} fetchScoreboard={fetchScoreboard}/>
}


const ScoreboardByAssignmentHook =(assignmentId : string) =>{
  const result= useGetScoreboardByAssignmentIdQuery({
    variables: { id: assignmentId }
  })

  return result

}

export default Scoreboard
