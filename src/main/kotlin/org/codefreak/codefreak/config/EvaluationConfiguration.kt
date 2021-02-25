package org.codefreak.codefreak.config

import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.service.evaluation.EvaluationQualifier
import org.codefreak.codefreak.service.evaluation.EvaluationQueue
import org.codefreak.codefreak.service.evaluation.EvaluationStepProcessor
import org.codefreak.codefreak.service.evaluation.EvaluationStepReader
import org.codefreak.codefreak.service.evaluation.EvaluationStepWriter
import org.springframework.batch.core.Job
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.configuration.JobRegistry
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.launch.support.SimpleJobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class EvaluationConfiguration {

  companion object {
    const val JOB_NAME = "evaluation"
    const val STEP_NAME = "evaluation"
    const val PARAM_EVALUATION_STEP_ID = "evaluationStepId"
  }

  @Autowired
  lateinit var config: AppConfiguration

  @Bean
  @EvaluationQualifier
  fun evaluationTaskExecutor(): TaskExecutor {
    val taskExecutor = ThreadPoolTaskExecutor()
    taskExecutor.setThreadNamePrefix("evaluation-")
    taskExecutor.corePoolSize = config.evaluation.maxConcurrentExecutions
    taskExecutor.maxPoolSize = config.evaluation.maxConcurrentExecutions
    taskExecutor.setQueueCapacity(config.evaluation.maxQueueSize)
    return taskExecutor
  }

  @Bean
  @EvaluationQualifier
  fun evaluationJobLauncher(jobRepository: JobRepository): JobLauncher {
    val jobLauncher = SimpleJobLauncher()
    jobLauncher.setTaskExecutor(evaluationTaskExecutor())
    jobLauncher.setJobRepository(jobRepository)
    jobLauncher.afterPropertiesSet()
    return jobLauncher
  }

  @Bean
  @EvaluationQualifier
  fun evaluationJob(
    jobBuilderFactory: JobBuilderFactory,
    stepBuilderFactory: StepBuilderFactory,
    reader: EvaluationStepReader,
    processor: EvaluationStepProcessor,
    writer: EvaluationStepWriter,
    queue: EvaluationQueue
  ): Job {

    val step = stepBuilderFactory.get(STEP_NAME)
        .chunk<EvaluationStep, EvaluationStep?>(5)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .listener(queue as StepExecutionListener)
        .build()

    return jobBuilderFactory.get(JOB_NAME)
        .incrementer(RunIdIncrementer())
        .start(step)
        .build()
  }

  @Bean
  fun jobRegistryBeanPostProcessor(jobRegistry: JobRegistry): JobRegistryBeanPostProcessor? {
    val jobRegistryBeanPostProcessor = JobRegistryBeanPostProcessor()
    jobRegistryBeanPostProcessor.setJobRegistry(jobRegistry)
    return jobRegistryBeanPostProcessor
  }
}
