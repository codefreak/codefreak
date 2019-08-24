package de.code_freak.codefreak.config

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.service.evaluation.AnswerProcessor
import de.code_freak.codefreak.service.evaluation.AnswerReader
import de.code_freak.codefreak.service.evaluation.EvaluationQualifier
import de.code_freak.codefreak.service.evaluation.EvaluationWriter
import org.springframework.batch.core.Job
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.batch.core.launch.support.SimpleJobLauncher
import org.springframework.batch.core.repository.JobRepository

@Configuration
class EvaluationConfiguration {
  @Autowired
  lateinit var config: AppConfiguration

  @Bean
  @EvaluationQualifier
  fun evaluationTaskExecutor(): TaskExecutor {
    val taskExecutor = ThreadPoolTaskExecutor()
    taskExecutor.setThreadNamePrefix("evaluation")
    taskExecutor.maxPoolSize = config.evalutaion.maxConcurrentExecutions
    taskExecutor.setQueueCapacity(config.evalutaion.maxQueueSize)
    return taskExecutor
  }

  @Bean
  @EvaluationQualifier
  fun evaluationJobLauncher(jobRepository: JobRepository): JobLauncher {
    val jobLauncher = SimpleJobLauncher()
    jobLauncher.setTaskExecutor(SimpleAsyncTaskExecutor())
    jobLauncher.setJobRepository(jobRepository)
    jobLauncher.afterPropertiesSet()
    return jobLauncher
  }

  @Bean
  @EvaluationQualifier
  fun evaluationJob(
    jobBuilderFactory: JobBuilderFactory,
    stepBuilderFactory: StepBuilderFactory,
    reader: AnswerReader,
    processor: AnswerProcessor,
    writer: EvaluationWriter
  ): Job {

    val step = stepBuilderFactory.get("evaluation")
        .chunk<Answer, Evaluation>(5)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build()

    return jobBuilderFactory.get("evaluation")
        .incrementer(RunIdIncrementer())
        .start(step)
        .build()
  }
}
