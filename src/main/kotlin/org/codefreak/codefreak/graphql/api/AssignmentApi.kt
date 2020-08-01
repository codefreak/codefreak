package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import com.expediagroup.graphql.spring.operations.Subscription
import graphql.schema.DataFetchingEnvironment
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.auth.Authorization
import org.codefreak.codefreak.auth.hasAuthority
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.AssignmentStatus
import org.codefreak.codefreak.graphql.BaseDto
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.graphql.ResolverContext
import org.codefreak.codefreak.service.AssignmentService
import org.codefreak.codefreak.service.GitImportService
import org.codefreak.codefreak.service.SubmissionService
import org.codefreak.codefreak.service.TaskService
import org.codefreak.codefreak.util.FrontendUtil
import org.codefreak.codefreak.util.TarUtil
import org.apache.catalina.core.ApplicationPart
import org.codefreak.codefreak.graphql.SubscriptionEventPublisher
import org.codefreak.codefreak.service.AssignmentStatusChangedEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID

@GraphQLName("Assignment")
class AssignmentDto(@GraphQLIgnore val entity: Assignment, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val title = entity.title
  val owner by lazy { UserDto(entity.owner, ctx) }
  val createdAt = entity.createdAt
  val deadline = entity.deadline
  val status by lazy { entity.status }
  val active = entity.active
  val openFrom = entity.openFrom
  val tasks by lazy { entity.tasks.map { TaskDto(it, ctx) } }
  val editable by lazy { entity.isEditable(authorization) }
  val exportUrl by lazy { FrontendUtil.getUriBuilder().path("/api/assignments/$id/export").build().toUriString() }
  val deletable by lazy {
    authorization.currentUser.hasAuthority(Authority.ROLE_ADMIN) || (authorization.isCurrentUser(entity.owner) &&
        (status != AssignmentStatus.OPEN))
  }

  val submissionsDownloadUrl by lazy { FrontendUtil.getUriBuilder().path("/api/assignments/$id/submissions").build().toUriString() }
  val submissions by lazy {
    authorization.requireAuthority(Authority.ROLE_TEACHER)
    serviceAccess.getService(SubmissionService::class)
        .findSubmissionsOfAssignment(id)
        .map { SubmissionDto(it, ctx) }
  }
}

@GraphQLName("AssignmentCreationResult")
class AssignmentCreationResultDto(@GraphQLIgnore val result: AssignmentService.AssignmentCreationResult, ctx: ResolverContext) : BaseDto(ctx) {
  val assignment by lazy { AssignmentDto(result.assignment, ctx) }
  val taskErrors by lazy { result.taskErrors.map { it.key + ": " + it.value.message }.toTypedArray() }
}

class AssignmentInput(val id: UUID, val active: Boolean, val deadline: Instant?, val openFrom: Instant?) {
  constructor() : this(UUID.randomUUID(), true, null, null)
}

@Component
class AssignmentQuery : BaseResolver(), Query {

  @Transactional
  @Secured(Authority.ROLE_STUDENT)
  fun assignments(): List<AssignmentDto> = context {
    val assignmentService = serviceAccess.getService(AssignmentService::class)
    val user = FrontendUtil.getCurrentUser()
    val assignments = when {
      authorization.currentUser.hasAuthority(Authority.ROLE_ADMIN)
          -> assignmentService.findAllAssignments()
      authorization.currentUser.hasAuthority(Authority.ROLE_TEACHER)
          -> assignmentService.findAssignmentsByOwner(authorization.currentUser)
      else -> assignmentService.findAllAssignmentsForUser(user.id)
    }
    assignments.map { AssignmentDto(it, this) }
  }

  @Transactional
  fun assignment(id: UUID): AssignmentDto = context {
    val assignment = serviceAccess.getService(AssignmentService::class).findAssignment(id)
    if (assignment.status == AssignmentStatus.INACTIVE) {
      authorization.requireAuthorityIfNotCurrentUser(assignment.owner, Authority.ROLE_ADMIN)
    }
    AssignmentDto(assignment, this)
  }
}

@Component
class AssignmentMutation : BaseResolver(), Mutation {

  @Secured(Authority.ROLE_TEACHER)
  fun createAssignment(): AssignmentDto = context {
    serviceAccess.getService(AssignmentService::class).createEmptyAssignment(authorization.currentUser).let { AssignmentDto(it, this) }
  }

  @Secured(Authority.ROLE_TEACHER)
  fun uploadAssignment(files: Array<ApplicationPart>): AssignmentCreationResultDto = context {
    ByteArrayOutputStream().use {
      TarUtil.writeUploadAsTar(files, it)
      val assignment = serviceAccess.getService(AssignmentService::class).createFromTar(it.toByteArray(), authorization.currentUser)
      AssignmentCreationResultDto(assignment, this)
    }
  }

  @Secured(Authority.ROLE_TEACHER)
  fun importAssignment(url: String): AssignmentCreationResultDto = context {
    ByteArrayOutputStream().use {
      serviceAccess.getService(GitImportService::class).importFiles(url, it)
      val assignment = serviceAccess.getService(AssignmentService::class).createFromTar(it.toByteArray(), authorization.currentUser)
      AssignmentCreationResultDto(assignment, this)
    }
  }

  fun deleteAssignment(id: UUID): Boolean = context {
    val assignment = serviceAccess.getService(AssignmentService::class).findAssignment(id)
    authorization.requireAuthorityIfNotCurrentUser(assignment.owner, Authority.ROLE_ADMIN)
    serviceAccess.getService(AssignmentService::class).deleteAssignment(assignment.id)
    true
  }

  fun updateAssignment(id: UUID, title: String, active: Boolean, deadline: Instant?, openFrom: Instant?): Boolean = context {
    val assignment = serviceAccess.getService(AssignmentService::class).findAssignment(id)
    authorization.requireAuthorityIfNotCurrentUser(assignment.owner, Authority.ROLE_ADMIN)
    assignment.title = title
    assignment.active = active
    assignment.deadline = deadline
    assignment.openFrom = openFrom
    serviceAccess.getService(AssignmentService::class).saveAssignment(assignment)
    true
  }

  fun addTasksToAssignment(assignmentId: UUID, taskIds: Array<UUID>): Boolean = context {
    val assignment = serviceAccess.getService(AssignmentService::class).findAssignment(assignmentId)
    val tasks = taskIds.map { serviceAccess.getService(TaskService::class).findTask(it) }
    tasks.forEach {
      authorization.requireAuthorityIfNotCurrentUser(it.owner, Authority.ROLE_ADMIN)
    }
    require(assignment.isEditable(authorization)) { "Assignment is not editable" }
    serviceAccess.getService(AssignmentService::class).addTasksToAssignment(assignment, tasks)
    true
  }
}

fun Assignment.isEditable(authorization: Authorization) = authorization.currentUser.hasAuthority(Authority.ROLE_ADMIN) ||
    authorization.isCurrentUser(owner)

@Component
class AssignmentStatusChangedEventPublisher : SubscriptionEventPublisher<AssignmentStatusChangedEvent>()

@Component
class AssignmentSubscription : BaseResolver(), Subscription {

  @Autowired
  private lateinit var assignmentStatusChangedEventPublisher: AssignmentStatusChangedEventPublisher

  fun assignmentStatusChange(assignmentId: UUID, env: DataFetchingEnvironment): Flux<AssignmentStatus> = context(env) {
    assignmentStatusChangedEventPublisher.eventStream
        .filter { it.assignmentId == assignmentId }
        .map { it.status }
  }
}