package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.*
import org.codefreak.codefreak.repository.*
import org.codefreak.codefreak.service.BaseService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import javax.annotation.PostConstruct

@Service
class GradeService : BaseService() {


  /**
   * Logging
   */
  val LOG = LoggerFactory.getLogger(this::class.simpleName)

  /**
   * Instance of GradeRepository for DB access
   */
  @Autowired
  private lateinit var gradeRepository : GradeRepository

  @Autowired
  private lateinit var poeStepService : PointsOfEvaluationStepService

  @Autowired
  private lateinit var stepRepository : EvaluationStepRepository

  @Autowired
  private lateinit var evaluationRepository : EvaluationRepository

  @PostConstruct
  fun initialize(){
    //function to integrate
  }

  /**
   * Has a trackback function to look for an valid Evaluation of a Grade.
   * There might be no evaluation present on first evaluationstep finishes. So if its null, there will be nothing to return.
   * Adjust GraphQL Query and handle null objects on Frontend.
   *
   */
  fun createOrUpdateGradeFromPointsOfEvaluation(poe : PointsOfEvaluationStep) : Boolean{
    val eval = trackBackToEvaluation(poe)
    return if(eval!=null){
      createOrUpdateGradeFromEvaluation(eval)
      true
    }else{
      false
    }
  }

  /**
   * Without trackback function if Evaluation is known
   * Will also be called from trackback containing function
   * run means if eval!=null. said simply comparison
   */
  fun createOrUpdateGradeFromEvaluation(eval : Evaluation) : Boolean{
    return run {
      startGradeCalculation(eval)
      true
    }
  }

  /**
   * function to start a GradeCalculation
   */
  fun startGradeCalculation(eval : Evaluation) : Grade?{
    val stepList = stepRepository.findAllByEvaluation(eval)
    val grade = findOrCreateGrade(eval)

    return if(validateEvaluationSteps(stepList)){
      val poeList = mutableListOf<PointsOfEvaluationStep>()

      //Add grade to PointsOfEvaluationStep. Afterwards an existing grade just can collects its PointsOfEvaluationStep Child and recalculate the grade
      for(s in stepList){
        var poe = poeStepService.findByEvaluationStep(s)
        poe.grade = grade
        poeList.add(poeStepService.save(poe))
      }
       calcGrade(grade,poeList)
    }else{
      null
    }
  }


  /**
   * Validates all EvaluationSteps. If there are Errors this function returns false
   * Decider if a grade will be calculated.
   *
   */
  private fun validateEvaluationSteps(steps : MutableList<EvaluationStep>) : Boolean{
    for(s in steps){
      if(s.result==null){return false}
      s.result?.let { if(it == EvaluationStepResult.ERRORED) return false }
    }
    return true
  }

  /**
   * Calculates a grade based on the points archived in a PointsOfEvaluationStep List
   * Sets grade.calculated on true at last so it will be considered for a grade.
   */
  private fun calcGrade(grade: Grade, poeList : MutableList<PointsOfEvaluationStep>) : Grade{
    var points = 0f
    var maxPoints = 0f
    for(poe in poeList){
      points+=poe.pOfE
      maxPoints+=poe.gradeDefinition.pEvalMax
    }
    LOG.info("points are $points and maxPoints are $maxPoints")
    grade.gradePercentage=(100/maxPoints*points)
    LOG.info("Grade is: ${grade.gradePercentage} of ${grade.evaluation.answer.submission.user}")
    grade.calculated=true
    return save(grade)
  }

  /**
   * Returns the best answer if one exists.
   */
  fun getBestGradeOfAnswer(answer : UUID) : Grade?{
    return gradeRepository.findFirstByAnswerIdOrderByGradePercentageDesc(answer).let {
      if(it.isPresent)
        it.get()
      else
        null
      }
  }

  /**
   * Saves a Grade
   */
  fun save(grade:Grade) = gradeRepository.save(grade)

  /**
   * Trackback function. Is required to recalculate a grade if a pointsOfEvaluationStep has been modified by a teacher
   */
  private fun trackBackToEvaluation(poe : PointsOfEvaluationStep) : Evaluation?{
    LOG.info("Trackback: from ${poe.id} to ${poe.evaluationStep}")
    return evaluationRepository.findByEvaluationSteps(poe.evaluationStep).let {
      if(it.isPresent){
        it.get()
      }else{
        null
      }
    }
  }

  /**
   * If a Grade is present pick it and return it. Otherwise create a new Grade for this evaluation / answer
   * @return New Grade or already existing one
   */
  fun findOrCreateGrade(evaluation : Evaluation) : Grade {
    gradeRepository.findByEvaluation(evaluation).let {
      //Check if grade is present. If not, create one, save it and perform a calculation by its Evaluation
      return if(!it.isPresent){
        save(Grade(evaluation,evaluation.answer))
      }else{
        it.get()
      }
    }

//    return gradeRepository.findByEvaluation(evaluation).get()
  }

  fun findGrade(evaluation: Evaluation) : Grade?{
    return gradeRepository.findByEvaluation(evaluation).let {
      if(it.isPresent)
        it.get()
      else
        null
    }
  }
}
