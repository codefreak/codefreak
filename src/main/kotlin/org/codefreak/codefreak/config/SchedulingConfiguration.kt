package org.codefreak.codefreak.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@ConditionalOnProperty(value = ["codefreak.scheduling.enable"], havingValue = "true", matchIfMissing = true)
@Configuration
@EnableScheduling
class SchedulingConfiguration
