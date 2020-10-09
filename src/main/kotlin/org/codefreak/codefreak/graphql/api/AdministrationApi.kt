package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import java.time.Instant
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.service.AdministrationService
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component

@GraphQLName("SystemConfig")
class SystemConfigDto(
  val motd: String?,
  val maxFileUploadSize: Long,
  val defaultIdeDockerImage: String
)

@GraphQLName("TimeSync")
class TimeSyncDto(
  val clientTimestamp: Long,
  val serverTimestamp: Long
)

@Component
class AdministrationQuery : BaseResolver(), Query {

  @Secured(Authority.ROLE_STUDENT)
  fun systemConfig(): SystemConfigDto {
    val adminService = context { serviceAccess.getService(AdministrationService::class) }

    return SystemConfigDto(
        motd = adminService.getMotd(),
        maxFileUploadSize = adminService.getMaxUploadSize(),
        defaultIdeDockerImage = adminService.getDefaultIdeDockerImageName()
    )
  }

  fun timeSync(clientTimestamp: Long): TimeSyncDto {
    return TimeSyncDto(
        clientTimestamp,
        Instant.now().toEpochMilli()
    )
  }
}
