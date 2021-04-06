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
import org.codefreak.codefreak.entity.GradingDefinition
import org.codefreak.codefreak.entity.PointsOfEvaluationStep
import org.codefreak.codefreak.entity.Submission
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.graphql.BaseDto
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.graphql.ResolverContext
import org.codefreak.codefreak.service.AliasNameGenerator
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.AssignmentService
import org.codefreak.codefreak.service.SubmissionService
import org.codefreak.codefreak.service.UserService
import org.codefreak.codefreak.service.evaluation.EvaluationService
import org.codefreak.codefreak.service.evaluation.GradeService
import org.codefreak.codefreak.service.evaluation.GradingDefinitionService
import org.codefreak.codefreak.service.evaluation.PointsOfEvaluationStepService
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component

@GraphQLName("GradingDefinition")
class GradingDefinitionDto(@GraphQLIgnore val entity: GradingDefinition, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val active = entity.active
  val maxPoints = entity.maxPoints
  val minorMistakePenalty = entity.minorMistakePenalty
  val majorMistakePenalty = entity.majorMistakePenalty
  val criticalMistakePenalty = entity.criticalMistakePenalty
}

@GraphQLName("PointsOfEvaluationStep")
class PointsOfEvaluationStepDto(@GraphQLIgnore val entity: PointsOfEvaluationStep?, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity?.id
  val mistakePoints = entity?.mistakePoints
  val reachedPoints = entity?.reachedPoints
  val calculationCheck = entity?.calculationCheck
  val edited = entity?.edited
  val evaluationStepResultCheck = entity?.evaluationStepResultCheck
  val gradingDefinition by lazy {
    entity?.id?.let {
      ctx.serviceAccess.getService(GradingDefinitionService::class).findByPointsOfEvaluationStepId(
        it
      )
    }?.let { GradingDefinitionDto(it, ctx) }
  }
}

@GraphQLName("Grade")
class GradeDto(@GraphQLIgnore val entity: Grade?, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity?.id
  val gradePercentage = entity?.gradePercentage
}

@GraphQLName("UserAlias")
class UserAliasDto(@GraphQLIgnore val entity: User, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val alias by lazy {
    if (entity.alias != null) { entity.alias } else { ctx.serviceAccess.getService(AliasNameGenerator::class).generateAndSetAlias(entity).alias }
  }
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
  val userAlias by lazy { UserAliasDto(entity.user, ctx) }
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
class GradeScoreboardDto(@GraphQLIgnore val entity: Grade?, ctx: ResolverContext) : BaseDto(ctx) {
  @GraphQLID
  val id = entity?.id
  val gradePercentage = entity?.gradePercentage
}
@GraphQLName("TaskScoreboard")
class TaskScoreboardDto(@GraphQLIgnore val entity: Task, ctx: ResolverContext) : BaseDto(ctx) {
  @GraphQLID
  val id = entity.id
  val title = entity.title
}

@GraphQLName("GradingDefinitionInput")
class GradingDefinitionInputDto(var id: UUID, var active: Boolean?, var maxPoints: Float, var minorMistakePenalty: Float, var majorMistakePenalty: Float, var criticalMistakePenalty: Float) {
  constructor() : this(UUID.randomUUID(), false, 0f, 0f, 0f, 0f)
}

@GraphQLName("PointsOfEvaluationStepInput")
class PointsOfEvaluationStepInputDto(var id: UUID, var reachedPoints: Float, var mistakePoints: Float, var calculationCheck: Boolean, var edited: Boolean, var evaluationStepResultCheck: Boolean) {
  constructor() : this(UUID.randomUUID(), 0f, 0f, false, false, false)
}

@GraphQLName("UserAliasInput")
class UserAliasInputDto(var id: UUID, var alias: String) {
  constructor() : this(UUID.randomUUID(), "Empty")
}

@Component
class GradingDefinitionQuery : BaseResolver(), Query {

  @Secured(Authority.ROLE_TEACHER)
  fun gradingDefinition(id: UUID): GradingDefinitionDto = context {
    val gradingDefinitionService = serviceAccess.getService(GradingDefinitionService::class)
    GradingDefinitionDto(gradingDefinitionService.findGradingDefinition(id), this)
  }

  @Secured(Authority.ROLE_TEACHER)
  fun gradingDefinitionByEvaluationStepDefinition(evaluationStepDefinitionId: UUID): GradingDefinitionDto = context {
    val gradingDefinitionService = serviceAccess.getService(GradingDefinitionService::class)
    GradingDefinitionDto(gradingDefinitionService.findGradingDefinitionByEvaluationStepDefinitionId(evaluationStepDefinitionId), this)
  }
}

@Component
class GradingDefinitionMutation : BaseResolver(), Mutation {

  @Secured(Authority.ROLE_TEACHER)
  fun updateGradingDefinition(id: UUID, active: Boolean?, maxPoints: Float?, minorMistakePenalty: Float?, majorMistakePenalty: Float?, criticalMistakePenalty: Float?): Boolean = context {
    val gradingDefinitionService = serviceAccess.getService(GradingDefinitionService::class)
    val gradingDefinition = gradingDefinitionService.findGradingDefinition(id)
    gradingDefinitionService.updateGradingDefinition(
      gradingDefinition,
      active, maxPoints, minorMistakePenalty, majorMistakePenalty, criticalMistakePenalty
    )
    true
  }

  @Secured(Authority.ROLE_TEACHER)
  fun updateGradingDefinitionValues(input: GradingDefinitionInputDto): Boolean = context {
    val gradingDefinitionService = serviceAccess.getService(GradingDefinitionService::class)
    val gradingDefinition = gradingDefinitionService.findGradingDefinition(input.id)
    gradingDefinitionService.updateGradingDefinition(
      gradingDefinition,
      active = input.active,
      maxPoints = input.maxPoints,
      minorMistakePenalty = input.minorMistakePenalty,
      majorMistakePenalty = input.majorMistakePenalty,
      criticalMistakePenalty = input.criticalMistakePenalty
    )
    true
  }
}

@Component
class PointsOfEvaluationStepQuery : BaseResolver(), Query {

  @Secured(Authority.ROLE_STUDENT)
  fun pointsOfEvaluation(id: UUID): PointsOfEvaluationStepDto = context {
    val pointsOfEvaluationStepService = serviceAccess.getService(PointsOfEvaluationStepService::class)
    val poe = pointsOfEvaluationStepService.findEvaluationStepById(id)
    PointsOfEvaluationStepDto(poe, this)
  }

  @Secured(Authority.ROLE_STUDENT)
  fun pointsOfEvaluationStepByEvaluationStepId(evaluationStepId: UUID): PointsOfEvaluationStepDto = context {
    val pointsOfEvaluationStepService = serviceAccess.getService(PointsOfEvaluationStepService::class)
    PointsOfEvaluationStepDto(pointsOfEvaluationStepService.findEvaluationStepById(evaluationStepId), this)
  }
}

@Component
class PointsOfEvaluationStepMutation : BaseResolver(), Mutation {

  @Secured(Authority.ROLE_TEACHER)
  fun updatePointsOfEvaluationStep(input: PointsOfEvaluationStepInputDto): Boolean = context {
    val poeService = serviceAccess.getService(PointsOfEvaluationStepService::class)
    val poe = poeService.findById(input.id)
      poeService.updatePointsOfEvaluationStep(poe,
        reachedPoints = input.reachedPoints,
        mistakePoints = input.mistakePoints,
        calculationCheck = input.calculationCheck,
        edited = input.edited,
        evaluationStepResultCheck = input.evaluationStepResultCheck
      )
      true
  }
}

@Component
class GradeQuery : BaseResolver(), Query {

  @Secured(Authority.ROLE_STUDENT)
  fun gradeForEvaluation(evaluationId: UUID): GradeDto? = context {
    val evaluationService = serviceAccess.getService(EvaluationService::class)
    val evaluation = evaluationService.getEvaluation(evaluationId)
    evaluation.grade?.let {
      GradeDto(it, this)
    }
  }
}

@Component
class UserAliasQuery : BaseResolver(), Query {

  @Secured(Authority.ROLE_STUDENT)
  fun userAliasByUserId(id: UUID): UserAliasDto = context {
    val userService = serviceAccess.getService(UserService::class)
    UserAliasDto(userService.getById(id), this)
  }
}

@Component
class UserAliasMutation : BaseResolver(), Mutation {

  @Secured(Authority.ROLE_STUDENT)
  fun updateUserAlias(input: UserAliasInputDto): Boolean = context {
    val service = serviceAccess.getService(UserService::class)
    val userAlias = service.getById(input.id)
    if (authorization.isCurrentUser(userAlias)) {
      if (!service.existsByAlias(input.alias)) {
        userAlias.alias = input.alias
        service.save(userAlias)
      }
    }
    true
  }
}

@Component
class ScoreboardQuery : BaseResolver(), Query {

  @Secured(Authority.ROLE_STUDENT)
  fun scoreboardByAssignmentId(id: UUID): AssignmentScoreboardDto = context {
    val assignmentService = serviceAccess.getService(AssignmentService::class)
    val assignment = assignmentService.findAssignment(id)
    AssignmentScoreboardDto(assignment, this)
  }
}
