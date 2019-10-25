package de.code_freak.codefreak.config

import de.code_freak.codefreak.auth.AuthenticationMethod
import de.code_freak.codefreak.auth.DevUserDetailsService
import de.code_freak.codefreak.auth.LdapUserDetailsContextMapper
import de.code_freak.codefreak.util.withTrailingSlash
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.PathRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.BeanIds
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider

@Configuration
class SecurityConfiguration : WebSecurityConfigurerAdapter() {

  @Autowired
  lateinit var config: AppConfiguration

  @Autowired
  lateinit var env: Environment

  @Autowired(required = false)
  var ldapUserDetailsContextMapper: LdapUserDetailsContextMapper? = null

  @Bean(BeanIds.AUTHENTICATION_MANAGER)
  override fun authenticationManagerBean(): AuthenticationManager = super.authenticationManagerBean()

  override fun configure(http: HttpSecurity?) {
    http
        ?.authorizeRequests()
            ?.requestMatchers(PathRequest.toStaticResources().atCommonLocations())?.permitAll()
            ?.antMatchers("/assets/**")?.permitAll()
            ?.antMatchers("/graphql/**")?.permitAll()
            ?.anyRequest()?.authenticated()
        ?.and()
            ?.formLogin()
                ?.loginPage("/login")
                ?.permitAll()
        ?.and()
            ?.logout()
            ?.permitAll()
        ?.and()
            ?.csrf()?.ignoringAntMatchers("/graphql")
  }
  @Bean
  override fun userDetailsService(): UserDetailsService {
    return when (config.authenticationMethod) {
      AuthenticationMethod.SIMPLE -> when (env.acceptsProfiles(Profiles.of("dev", "test"))) {
        true -> DevUserDetailsService()
        false -> throw NotImplementedError("Simple authentication is currently only supported in dev mode.")
      }
      else -> super.userDetailsService()
    }
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
