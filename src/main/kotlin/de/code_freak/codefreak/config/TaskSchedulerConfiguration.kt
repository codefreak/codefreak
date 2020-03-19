package de.code_freak.codefreak.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
class TaskSchedulerConfiguration {
  @Bean
  fun threadPoolTaskScheduler(): ThreadPoolTaskScheduler? {
    val threadPoolTaskScheduler = ThreadPoolTaskScheduler()
    threadPoolTaskScheduler.poolSize = 5
    threadPoolTaskScheduler.isRemoveOnCancelPolicy = true
    threadPoolTaskScheduler.setThreadNamePrefix("CodeFreakScheduled")
    return threadPoolTaskScheduler
  }
}