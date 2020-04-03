package org.codefreak.codefreak.config

import org.codefreak.codefreak.auth.AuthenticationMethod
import org.codefreak.codefreak.auth.SimpleUserDetailsService
import org.codefreak.codefreak.auth.LdapUserDetailsContextMapper
import org.codefreak.codefreak.service.UserService
import org.codefreak.codefreak.util.withTrailingSlash
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.PathRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.BeanIds
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.web.session.HttpSessionEventPublisher
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.security.config.annotation.web.builders.WebSecurity

@Configuration
class SecurityConfiguration : WebSecurityConfigurerAdapter() {

  @Autowired
  lateinit var config: AppConfiguration

  @Autowired
  lateinit var userService: UserService

  @Autowired(required = false)
  var ldapUserDetailsContextMapper: LdapUserDetailsContextMapper? = null

  @Bean(BeanIds.AUTHENTICATION_MANAGER)
  override fun authenticationManagerBean(): AuthenticationManager = super.authenticationManagerBean()

  override fun configure(web: WebSecurity?) {
    // disable web security for all static files
    web?.ignoring()
        ?.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
        ?.antMatchers("/assets/**")
        // all static files served from React – this is far from being perfect.
        // There should be something like "all files from /static directory are allowed" but security is based
        // on the HTTP Request and not on actual files
        ?.antMatchers("/*.*", "/static/**")
  }

  override fun configure(http: HttpSecurity?) {
    http
        ?.authorizeRequests()
            // use /api/... for non-GraphQL API resources that require authentication
            ?.antMatchers("/api/**")?.authenticated()
            ?.anyRequest()?.permitAll()
        ?.and()
            ?.formLogin()
                // force redirect to React's login page
                ?.loginPage("/login")
        ?.and()
            ?.csrf()?.ignoringAntMatchers("/graphql")
    http?.sessionManagement()
        ?.maximumSessions(1)
        ?.sessionRegistry(sessionRegistry())
  }

  @Bean
  override fun userDetailsService(): UserDetailsService {
    return when (config.authenticationMethod) {
      AuthenticationMethod.SIMPLE -> SimpleUserDetailsService(userService)
      else -> super.userDetailsService()
    }
  }

  @Bean
  fun sessionRegistry(): SessionRegistry {
    return SessionRegistryImpl()
  }

  @Bean
  fun httpSessionEventPublisher(): ServletListenerRegistrationBean<HttpSessionEventPublisher> {
    return ServletListenerRegistrationBean(HttpSessionEventPublisher())
  }

  override fun configure(auth: AuthenticationManagerBuilder?) {
    when (config.authenticationMethod) {
      AuthenticationMethod.LDAP -> configureLdapAuthentication(auth)
      else -> super.configure(auth)
    }
  }

  private fun configureLdapAuthentication(auth: AuthenticationManagerBuilder?) {
    val url = config.ldap.url ?: throw IllegalStateException("LDAP URL has not been configured")
    if (config.ldap.activeDirectory) {
      val authenticationProvider = ActiveDirectoryLdapAuthenticationProvider(null, url, config.ldap.rootDn)
      authenticationProvider.setUserDetailsContextMapper(ldapUserDetailsContextMapper)
      auth?.authenticationProvider(authenticationProvider)
      return
    }
    auth?.ldapAuthentication()
        ?.userDetailsContextMapper(ldapUserDetailsContextMapper)
        ?.userSearchBase(config.ldap.userSearchBase)
        ?.userSearchFilter(config.ldap.userSearchFilter)
        ?.groupSearchBase(config.ldap.groupSearchBase)
        ?.groupSearchFilter(config.ldap.groupSearchFilter)
        ?.contextSource()
            ?.url(url.withTrailingSlash() + config.ldap.rootDn)
  }
}
