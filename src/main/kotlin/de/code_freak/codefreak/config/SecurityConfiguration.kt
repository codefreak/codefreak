package de.code_freak.codefreak.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
class SecurityConfiguration : WebSecurityConfigurerAdapter() {

  override fun configure(http: HttpSecurity?) {
    http?.authorizeRequests()
        ?.antMatchers("/admin/**")?.authenticated()
        ?.anyRequest()?.permitAll()
        ?.and()
        ?.httpBasic()
  }
}
