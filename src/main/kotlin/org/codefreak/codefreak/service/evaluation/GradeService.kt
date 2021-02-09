package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.*
import org.codefreak.codefreak.repository.*
import org.codefreak.codefreak.service.BaseService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

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



//
//  @PostConstruct
  fun initialize() {
    //Once and For All process a grade for all valid evaluations
    val evalList = evaluationRepository.findAll()
    for(eval in evalList){
      LOG.info("generate Grade for Evaluation with id ${eval.id}")
      createOrUpdateGradeFromEvaluation(eval)
    }

  }

  /**
   * Without trackback function. Collects all persistent Evaluations and generates a grade.
   */
  fun createOrUpdateGradeFromEvaluation(eval : Evaluation){
    startGradeCalculation(eval)
  }


  /**
   * Has a trackback function. Should be the pick if points get edited manually
   */
  fun createOrUpdateGradeFromPointsOfEvaluation(poe : PointsOfEvaluationStep){
    var eval = trackBackToEvaluation(poe)
    createOrUpdateGradeFromEvaluation(eval)

  }

  /**
   * function to start a GradeCalculation
   */
  fun startGradeCalculation(eval : Evaluation) {
    val stepList = stepRepository.findAllByEvaluation(eval)
    val grade = findOrCreateGrade(eval)

    if(validateEvaluationSteps(stepList)){
      val poeList = mutableListOf<PointsOfEvaluationStep>()

      //Add grade to PointsOfEvaluationStep. Afterwards an existing grade just can collects its PointsOfEvaluationStep Child and recalculate the grade
      for(s in stepList){
        var poe = poeStepService.findByEvaluationStep(s)
        poe.grade = grade
        poeList.add(poeStepService.save(poe))
      }
      calcGrade(grade,poeList)
    }
  }

  /**
   * Updated a grade from its respective Evaluation
   */
  fun updateGradeCalculation(eval : Evaluation) {
    val grade = findOrCreateGrade(eval)
    save(grade)
  }

  /**
   * Validates all EvaluationSteps. If there are Errors this function returns false
   * Decider if a grade will be calculated
   */
  private fun validateEvaluationSteps(steps : MutableList<EvaluationStep>) : Boolean{
    for(s in steps){
      if(s.result!! != EvaluationStepResult.SUCCESS) return false
    }
    return true
  }

  /**
   * Calculates a grade based on the points archieved in a PointsOfEvaluationStep List
   * Sets grade.calculated on true at last so it will be considered for a grade.
   */
  private fun calcGrade(grade: Grade, poeList : MutableList<PointsOfEvaluationStep>) {
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
    save(grade)
  }

  /**
   * Returns the best answer if one exists.
   */
  fun getBestGradeOfAnswer(answer : UUID) : Grade?{
    return if(gradeRepository.findFirstByAnswerIdOrderByGradePercentageDesc(answer).isPresent){
      gradeRepository.findFirstByAnswerIdOrderByGradePercentageDesc(answer).get()
    }else{
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
  private fun trackBackToEvaluation(poe : PointsOfEvaluationStep) : Evaluation{
    LOG.info("Trackback: from ${poe.id} to ${poe.evaluationStep}")
//    val step = stepRepository.findByPointsOfEvaluationStep(poe).get()
    return evaluationRepository.findByEvaluationSteps(poe.evaluationStep).get()
  }

  /**
   * If a Grade is present pick it and return it. Otherwise create a new Grade for this evaluation / answer
   * @return New Grade or already existing one
   */
  fun findOrCreateGrade(evaluation : Evaluation) : Grade {
    if(!gradeRepository.findByEvaluation(evaluation).isPresent){
      save(Grade(evaluation,evaluation.answer))
      startGradeCalculation(evaluation)
//      evaluationRepository.save(evaluation)
      }
    return gradeRepository.findByEvaluation(evaluation).get()
  }
}
