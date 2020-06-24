package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.service.AdministrationService
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component

@GraphQLName("SystemConfig")
class SystemConfigDto(
  val motd: String?,
  val maxFileUploadSize: Long
)

@Component
class AdministrationQuery : BaseResolver(), Query {

  @Secured(Authority.ROLE_STUDENT)
  fun systemConfig(): SystemConfigDto {
    val adminService = context { serviceAccess.getService(AdministrationService::class) }

    return SystemConfigDto(
        motd = adminService.getMotd(),
        maxFileUploadSize = adminService.getMaxUploadSize()
    )
  }
}
