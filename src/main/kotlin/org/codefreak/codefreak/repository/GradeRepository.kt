package org.codefreak.codefreak.repository

import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.Grade
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*


/**
 * Repository of Grade. Associated generic functions added
 */
@Repository
interface GradeRepository : CrudRepository<Grade,UUID>{
  fun findByEvaluation(evaluation:Evaluation) : Optional<Grade>
  fun findFirstByAnswerIdOrderByGradePercentageDesc(id : UUID) : Optional<Grade>
}


