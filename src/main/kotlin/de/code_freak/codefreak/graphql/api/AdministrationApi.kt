package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.graphql.BaseResolver
import de.code_freak.codefreak.service.AdministrationService
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component

@Component
class AdministrationQuery : BaseResolver(), Query {

  @Secured(Authority.ROLE_STUDENT)
  fun motd() = context { serviceAccess.getService(AdministrationService::class).getMotd() }
}
