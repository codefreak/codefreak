package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import java.util.UUID
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.Grade
import org.codefreak.codefreak.entity.GradeDefinition
import org.codefreak.codefreak.entity.PointsOfEvaluationStep
import org.codefreak.codefreak.entity.Submission
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.UserAlias
import org.codefreak.codefreak.graphql.BaseDto
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.graphql.ResolverContext
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.AssignmentService
import org.codefreak.codefreak.service.SubmissionService
import org.codefreak.codefreak.service.UserAliasService
import org.codefreak.codefreak.service.evaluation.EvaluationService
import org.codefreak.codefreak.service.evaluation.GradeDefinitionService
import org.codefreak.codefreak.service.evaluation.GradeService
import org.codefreak.codefreak.service.evaluation.PointsOfEvaluationStepService
import org.slf4j.LoggerFactory
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component

/**
 * GradeDefinition for query
 */
@GraphQLName("GradeDefinition")
class GradeDefinitionDto(@GraphQLIgnore val entity: GradeDefinition, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val active = entity.active
  val pEvalMax = entity.pEvalMax
  val bOnMinor = entity.bOnMinor
  val bOnMajor = entity.bOnMajor
  val bOnCritical = entity.bOnCritical
}

/**
 * GradeDefinitionMaxDto for query. Main purpose
 */
@GraphQLName("GradeDefinitionMax")
class GradeDefinitionMaxDto(@GraphQLIgnore val entity: GradeDefinition, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val pEvalMax = entity.pEvalMax
  val active = entity.active
}

/**
 * PointsOfEvaluation Dto for query
 */
@GraphQLName("PointsOfEvaluationStep")
class PointsOfEvaluationStepDto(@GraphQLIgnore val entity: PointsOfEvaluationStep?, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity?.id
  val bOfT = entity?.bOfT
  val pOfE = entity?.pOfE
  val calcCheck = entity?.calcCheck
  val edited = entity?.edited
  val resultCheck = entity?.resultCheck
  val gradeDefinitionMax by lazy {
    entity?.id?.let {
      ctx.serviceAccess.getService(GradeDefinitionService::class).findByPointsOfEvaluationStepId(
        it
      )
    }?.let { GradeDefinitionMaxDto(it, ctx) }
  }
}

/**
 * Grade Dto for query
 */
@GraphQLName("Grade")
class GradeDto(@GraphQLIgnore val entity: Grade?, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity?.id
  val gradePercentage = entity?.let { it.gradePercentage }
  val calculated = entity?.let { it.calculated }
}

@GraphQLName("UserAlias")
class UserAliasDto(@GraphQLIgnore val entity: UserAlias, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val alias = entity.alias
}

/**
 *
 * ScoreBoard Dtos
 * More comfortable access if mapped completely down the tree with only required fields
 * All classes get a scoreboard postfix
 */
@GraphQLName("AssignmentScoreboard")
class AssignmentScoreboardDto(@GraphQLIgnore val entity: Assignment, ctx: ResolverContext) : BaseDto(ctx) {
  @GraphQLID
  val id = entity.id
  val title = entity.title
  val submissionsScoreboard by lazy { serviceAccess.getService(SubmissionService::class).findSubmissionsOfAssignment(id).map { SubmissionsScoreboardDto(it, ctx) } }
  val tasksScoreboard by lazy { entity.tasks.map { TaskScoreboardDto(it, ctx) } }
}

@GraphQLName("SubmissionsScoreboard")
class SubmissionsScoreboardDto(@GraphQLIgnore val entity: Submission, ctx: ResolverContext) : BaseDto(ctx) {
  @GraphQLID
  val id = entity.id
  // User is skipped in this Step. Not required on scoreboard UserAlias should be persistent
  val useralias by lazy { UserAliasDto(entity.user.userAlias!!, ctx) }
  val answersScoreboard by lazy { serviceAccess.getService(AnswerService::class).findAllBySubmissionId(id).map { AnswersScoreboardDto(it, ctx) } }
}

@GraphQLName("AnswersScoreboard")
class AnswersScoreboardDto(@GraphQLIgnore val entity: Answer, ctx: ResolverContext) : BaseDto(ctx) {
  @GraphQLID
  val id = entity.id
  val taskScoreboard by lazy { TaskScoreboardDto(entity.task, ctx) }
  val gradeScoreboard by lazy {
    serviceAccess.getService(GradeService::class).getBestGradeOfAnswer(id)?.let {
      GradeScoreboardDto(it, ctx) }
    }
}

@GraphQLName("GradeScoreboard")
class GradeScoreboardDto(@GraphQLIgnore val entity: Grade, ctx: ResolverContext) : BaseDto(ctx) {
  @GraphQLID
  val id = entity.id
  val gradePercentage = entity.gradePercentage
  val calculated = entity.calculated
}
@GraphQLName("TaskScoreboard")
class TaskScoreboardDto(@GraphQLIgnore val entity: Task, ctx: ResolverContext) : BaseDto(ctx) {
  @GraphQLID
  val id = entity.id
  val title = entity.title
}

/**
 * Dto to receive input for Gradedefinitions
 */
@GraphQLName("GradeDefinitionInput")
class GradeDefinitionInputDto(var id: UUID, var pEvalMax: Float, var bOnMinor: Float, var bOnMajor: Float, var bOnCritical: Float) {
  constructor() : this(UUID.randomUUID(), 0f, 0f, 0f, 0f)
}

@GraphQLName("GradeDefinitionActiveInput")
class GradeDefinitionActiveInputDto(var id: UUID, var active: Boolean) {
  constructor() : this(UUID.randomUUID(), false)
}

/**
 * Dto to receive input for PointsOfEvaluationSteps
 */
@GraphQLName("PointsOfEvaluationStepInput")
class PointsOfEvaluationStepInputDto(var id: UUID, var pOfE: Float, var bOfT: Float, var calcCheck: Boolean, var edited: Boolean, var resultCheck: Boolean) {
  constructor() : this(UUID.randomUUID(), 0f, 0f, false, false, false)
}

/**
 * Dto to receive input for UserAlias
 * Required to update the name
 */
@GraphQLName("UserAliasInput")
class UserAliasInputDto(var id: UUID, var alias: String) {
  constructor() : this(UUID.randomUUID(), "Empty")
}

@Component
class GradeDefinitionQuery : BaseResolver(), Query {
  /**
   * Logging
   */
  val LOG = LoggerFactory.getLogger(this::class.simpleName)

  /**
   *  Gets a GradeDefinition by its Id
   */
  @Secured(Authority.ROLE_TEACHER)
  fun gradeDefinition(id: UUID): GradeDefinitionDto = context {
    val gradeDefinitionService = serviceAccess.getService(GradeDefinitionService::class)
    LOG.info("generate GradeDefinitionDTO for Query")
    GradeDefinitionDto(gradeDefinitionService.findGradeDefinition(id), this)
  }

  /**
   * Gets Gradedefinition by EvaluationStepDefinitionId
   */
  @Secured(Authority.ROLE_TEACHER)
  fun gradeDefinitionByEvaluationStepDefinition(id: UUID): GradeDefinitionDto = context {
    val gradeDefinitionService = serviceAccess.getService(GradeDefinitionService::class)
    LOG.info("generate GradeDefinitionDTO for Query")
    GradeDefinitionDto(gradeDefinitionService.findGradeDefinitionByEvaluationStepDefinitionId(id), this)
  }
}

@Component
class GradeDefinitionMutation : BaseResolver(), Mutation {

  /**
   * Logging
   */
  val LOG = LoggerFactory.getLogger(GradeDefinitionMutation::class.simpleName)

  /**
   * Updates a Gradedefinition from its InputDto. This function fires a recalculation from all related grades.
   * May take some time. Thread candidate?
   */
  @Secured(Authority.ROLE_TEACHER)
  fun updateGradeDefinitionValues(input: GradeDefinitionInputDto): Boolean = context {
    val gradeDefinitionService = serviceAccess.getService(GradeDefinitionService::class)
    val gradeDefinition = gradeDefinitionService.findGradeDefinition(input.id)

    if (input.id == gradeDefinition.id) {
      gradeDefinitionService.updateGradeDefinitionValues(
        gradeDefinition,
        pEvalMax = input.pEvalMax,
        bOnMinor = input.bOnMinor,
        bOnMajor = input.bOnMajor,
        bOnCritical = input.bOnCritical
      )
      LOG.info("GradeDefinition values of $gradeDefinition updated")
      true
    } else
      LOG.error("failed to update GradeDefinition of id " + input.id)
      false
  }

  @Secured(Authority.ROLE_TEACHER)
  fun updateGradeDefinitionStatus(input: GradeDefinitionActiveInputDto): Boolean = context {
    val gradeDefinitionService = serviceAccess.getService(GradeDefinitionService::class)
    val gradeDefinition = gradeDefinitionService.findGradeDefinition(input.id)

    if (input.id == gradeDefinition.id) {
      gradeDefinitionService.updateGradeDefinitionStatus(gradeDefinition,
        active = input.active)
      LOG.info("GradeDefinition status of $gradeDefinition updated")
      true
    } else
      LOG.error("failed to update GradeDefinition of id " + input.id)
      false
  }
}

@Component
class PointsOfEvaluationStepQuery : BaseResolver(), Query {

  /**
   * Logging
   */
  val LOG = LoggerFactory.getLogger(this::class.simpleName)

  /**
   * Gets PointsOfEvaluationStep by its Id.
   */
  fun pointsOfEvaluation(id: UUID): PointsOfEvaluationStepDto = context {
    val pointsOfEvaluationStepService = serviceAccess.getService(PointsOfEvaluationStepService::class)
    LOG.info("generate PointsOfEvaluationStepDto for Query")
    val poe = pointsOfEvaluationStepService.getEvaluationStepId(id)!!
    PointsOfEvaluationStepDto(poe, this)
  }

  /**
   * Gets PointsOfEvaluationStep by its EvaluationStepId
   */
  @Secured(Authority.ROLE_STUDENT)
  fun pointsOfEvaluationStepByEvaluationStepId(id: UUID): PointsOfEvaluationStepDto = context {
    val pointsOfEvaluationStepService = serviceAccess.getService(PointsOfEvaluationStepService::class)
    PointsOfEvaluationStepDto(pointsOfEvaluationStepService.getEvaluationStepId(id), this)
  }
}

@Component
class PointsOfEvaluationStepMutation : BaseResolver(), Mutation {

  /**
   * Logging
   */
  val LOG = LoggerFactory.getLogger(this::class.simpleName)

  /**
   * Updates PointsOfEvaluationStep by its InputDto
   */
  @Secured(Authority.ROLE_TEACHER)
  fun updatePointsOfEvaluationStep(input: PointsOfEvaluationStepInputDto): Boolean = context {
    val poeService = serviceAccess.getService(PointsOfEvaluationStepService::class)
    val poe = poeService.findById(input.id)
    if (poe.id == input.id) {
      var updatedPoe = poeService.updatePointsOfEvaluationStep(poe,
        pOfE = input.pOfE,
        bOfT = input.bOfT,
        calcCheck = input.calcCheck,
        edited = input.edited,
        resultCheck = input.resultCheck
      )
      LOG.info("PointsOfEvaluationStep Updated ")
      // Moved to PointsOfEvaluationService
      // serviceAccess.getService(GradeService::class).createOrUpdateGradeFromPointsOfEvaluation(updatedPoe)
      true
    } else {
      LOG.error("failed to update PointsOfEvaluationStep of id ${input.id}")
      false
    }
  }
}

@Component
class GradeQuery : BaseResolver(), Query {

  /**
   * Logging
   */
  val LOG = LoggerFactory.getLogger(this::class.simpleName)

  /**
   * Gets a Grade from an Evaluation Id.
   */
  fun grade(id: UUID): GradeDto = context {
    val evaluationService = serviceAccess.getService(EvaluationService::class)
    val evaluation = evaluationService.getEvaluation(id)
    val gradeService = serviceAccess.getService(GradeService::class)
    GradeDto(gradeService.findGrade(evaluation), this)
  }
}

@Component
class UserAliasQuery : BaseResolver(), Query {

  /**
   * Logging
   */
  val LOG = LoggerFactory.getLogger(this::class.simpleName)

  fun userAliasByUserId(id: UUID): UserAliasDto = context {
    val userAliasService = serviceAccess.getService(UserAliasService::class)
    UserAliasDto(userAliasService.getByUserId(id), this)
  }
}

@Component
class UserAliasMutation : BaseResolver(), Mutation {

  /**
   * Logging
   */
  val LOG = LoggerFactory.getLogger(this::class.simpleName)

  fun updateUserAlias(input: UserAliasInputDto): Boolean = context {
    val service = serviceAccess.getService(UserAliasService::class)
    val userAlias = service.getById(input.id)
    if (input.id == userAlias.id) {
      if (!service.existsByAlias(input.alias)) {
        userAlias.alias = input.alias
        service.save(userAlias)
        true
      } else {
        false
      }
    } else {
      false
    }
  }
}

@Component
class ScoreboardQuery : BaseResolver(), Query {

  /**
   * Logging
   */
  val LOG = LoggerFactory.getLogger(this::class.simpleName)

  fun scoreboardByAssignmentId(id: UUID): AssignmentScoreboardDto = context {
    val assignmentService = serviceAccess.getService(AssignmentService::class)
    val assignment = assignmentService.findAssignmentById(id)

    AssignmentScoreboardDto(assignment, this)
  }
}
