package de.code_freak.codefreak.init

import com.beust.klaxon.Klaxon
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Classroom
import de.code_freak.codefreak.entity.Task
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.AssignmentRepository
import de.code_freak.codefreak.repository.ClassroomRepository
import de.code_freak.codefreak.repository.TaskRepository
import de.code_freak.codefreak.repository.UserRepository
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.TarUtil
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.lang.Exception
import javax.annotation.PostConstruct

@Component
@ConditionalOnProperty("code-freak.disk-import.enabled")
class DiskImport {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Value("\${code-freak.disk-import.root-directory}")
  private lateinit var rootDirectoryPath: String

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var assignmentRepository: AssignmentRepository

  @Autowired
  private lateinit var taskRepository: TaskRepository

  @Autowired
  private lateinit var classroomRepository: ClassroomRepository

  @Autowired
  private lateinit var fileService: FileService

  private var importInCurrentIteration = mutableListOf<String>()
  private var user = User("disk-import")
  private var classroom = Classroom("Import Classroom")

  @PostConstruct
  protected fun init() {
    log.info("Assignment import from disk is enabled. Root directory: $rootDirectoryPath")
    user = userRepository.save(user)
    classroom = classroomRepository.save(classroom)
  }

  @Scheduled(initialDelay = 1000*60*3, fixedDelay = 1000*60*3)
  fun importAssignmentFromDisk() {
    val rootDirectory = File(rootDirectoryPath)
    if (!rootDirectory.exists() || !rootDirectory.isDirectory) {
      log.debug("Skipping disk import because directory does not exist.")
      return
    }
    val importInNextIteration = mutableListOf<String>()
    for (assignmentDirectory in rootDirectory.listFiles()) {
      if (!assignmentDirectory.isDirectory) continue
      if (!importInCurrentIteration.contains(assignmentDirectory.name)) {
        log.info("Detected new directory ${assignmentDirectory.absolutePath}. Assignment will be imported after some safety margin time.")
        importInNextIteration.add(assignmentDirectory.name)
        continue
      }
      log.info("Importing assignment from ${assignmentDirectory.absolutePath}")
      try {
        // Klaxon doesn't close the file properly so it cannot be deleted afterwards
        val assignmentDefinitionJson = FileUtils.readFileToString(assignmentDirectory.resolve("assignment.json"), "UTF-8")
        val assignmentDefinition = Klaxon().parse<AssignmentDefinition>(assignmentDefinitionJson)
            ?: throw RuntimeException("assignment.json could not be parsed")
        val assignment = Assignment(assignmentDefinition.name, user, classroom)
        assignmentRepository.save(assignment)
        var nextPosition = 0L
        for (taskDefinition in assignmentDefinition.tasks) {
          log.info("Importing task $nextPosition: ${taskDefinition.title}")
          var task = Task(assignment, nextPosition++, taskDefinition.title, taskDefinition.description, 100)
          task = taskRepository.save(task)
          fileService.writeCollectionTar(task.id).use {
            TarUtil.createTarFromDirectory(assignmentDirectory.resolve(taskDefinition.directory), it)
          }
        }
      } catch (e: Exception) {
        log.error("Assignment could not be imported", e)
      } finally {
        log.info("Deleting directory ${assignmentDirectory.absolutePath}")
        FileUtils.deleteDirectory(assignmentDirectory)
      }
    }
    importInCurrentIteration = importInNextIteration
  }

  protected class AssignmentDefinition (val name: String, val tasks: Array<TaskDefinition>)

  protected class TaskDefinition(val title: String, val directory: String, val description: String? = null)
}
