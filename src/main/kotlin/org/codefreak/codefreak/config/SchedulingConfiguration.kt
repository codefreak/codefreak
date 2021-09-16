package org.codefreak.codefreak.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@ConditionalOnProperty(value = ["codefreak.scheduling.enable"], havingValue = "true", matchIfMissing = true)
@Configuration
@EnableScheduling
class SchedulingConfiguration {
  @Bean
  fun threadPoolTaskScheduler(): ThreadPoolTaskScheduler {
    val threadPoolTaskScheduler = ThreadPoolTaskScheduler()
    threadPoolTaskScheduler.poolSize = 5
    threadPoolTaskScheduler.isRemoveOnCancelPolicy = true
    threadPoolTaskScheduler.setThreadNamePrefix("CodeFreakScheduled")
    return threadPoolTaskScheduler
  }
}
