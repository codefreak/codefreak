package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.spring.operations.Query
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.service.AdministrationService
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component

@Component
class AdministrationQuery : BaseResolver(), Query {

  @Secured(Authority.ROLE_STUDENT)
  fun motd() = context { serviceAccess.getService(AdministrationService::class).getMotd() }
}
