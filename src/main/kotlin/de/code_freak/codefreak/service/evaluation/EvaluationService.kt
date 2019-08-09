package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.service.BaseService
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameter
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EvaluationService : BaseService() {

  @Autowired
  @EvaluationQualifier
  private lateinit var job: Job

  @Autowired
  @EvaluationQualifier
  private lateinit var jobLauncher: JobLauncher


  fun queueEvaluation(taskId: UUID) {
    val params = mapOf("answerId" to JobParameter(taskId.toString()))
    jobLauncher.run(job, JobParameters(params))
  }
}
