package org.codefreak.codefreak.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import liquibase.util.StreamUtil
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.TarUtil.getCodefreakDefinition
import org.codefreak.templates.TaskTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskTarService : BaseService() {

  @Autowired
  @Qualifier("yamlObjectMapper")
  private lateinit var yamlMapper: ObjectMapper

  @Autowired
  private lateinit var taskService: TaskService

  @Autowired
  private lateinit var fileService: FileService

  /**
   * Creates and saves a new task from the given tar.
   *
   * @param tarContent the tar containing a task
   * @param owner the owner of the task
   * @param assignment the assignment the task belongs to, if any
   * @param position the position of the task if it belongs to an assignment
   * @return the created task
   */
  @Transactional
  fun createFromTar(tarContent: ByteArray, owner: User, assignment: Assignment? = null, position: Long = 0L): Task {
    val definition = yamlMapper.getCodefreakDefinition<TaskDefinition>(tarContent.inputStream())
    var task = definition.toEntity(assignment, owner, position)
    task = taskService.saveTask(task)
    copyTaskFilesFromTar(task.id, tarContent)
    return task
  }

  private fun copyTaskFilesFromTar(taskId: UUID, tarContent: ByteArray) {
    fileService.writeCollectionTar(taskId).use { fileCollection ->
      TarUtil.copyEntries(
          TarArchiveInputStream(tarContent.inputStream()),
          TarUtil.PosixTarArchiveOutputStream(fileCollection)
      ) { !TarUtil.isCodefreakDefinition(it) }
    }
  }

  fun createFromTemplateName(templateName: String, owner: User): Task {
    val template = try {
      TaskTemplate.valueOf(templateName.uppercase())
    } catch (e: IllegalArgumentException) {
      throw EntityNotFoundException("Template $templateName does not exist!")
    }
    return createFromTemplate(template, owner)
  }

  fun createFromTemplate(template: TaskTemplate, owner: User): Task {
    val templateContent = template.archiveStream.use { it.readBytes() }
    return createFromTar(templateContent, owner)
  }

  /**
   * Creates and saves multiple tasks from the given tar.
   * The tar has to contain the individual tasks as tar archives themselves.
   *
   * @param tarContent the tar containing multiple tasks as tar archives
   * @param owner the owner of the tasks
   * @param assignment the assignment the tasks belong to, if any
   * @return the created tasks.
   */
  fun createMultipleFromTar(tarContent: ByteArray, owner: User, assignment: Assignment? = null): List<Task> {
    val input = TarArchiveInputStream(ByteArrayInputStream(tarContent))
    val tasks = mutableListOf<Task>()
    generateSequence { input.nextTarEntry }
        .filter { it.isFile }
        .forEach {
          val content = getArchiveContent(it, input)
          tasks.add(createFromTar(content, owner, assignment))
        }
    return tasks
  }

  private fun getArchiveContent(entry: TarArchiveEntry, inputStream: InputStream): ByteArray {
    if (!entry.name.endsWith(".tar")) {
      ByteArrayOutputStream().use {
        // Throws an exception if it is an unsupported archive type
        TarUtil.archiveToTar(inputStream, it)
        return it.toByteArray()
      }
    }

    return IOUtils.toByteArray(inputStream)
  }

  /**
   * Creates an empty task for the given User.
   *
   * @param owner the user who will own the task
   * @return the created task
   */
  @Transactional
  fun createEmptyTask(owner: User): Task {
    return ByteArrayOutputStream().use {
      StreamUtil.copy(ClassPathResource("empty_task.tar").inputStream, it)
      createFromTar(it.toByteArray(), owner)
    }
  }

  /**
   * Creates a tar archive of the task with the given id.
   *
   * @param taskId the id of the task to be exported
   * @return a tar archive containing the task
   */
  @Transactional
  fun getExportTar(taskId: UUID) = getExportTar(taskService.findTask(taskId))

  /**
   * Creates a tar archive of the given task.
   *
   * @param task the task to be exported
   * @return a tar archive containing the task
   */
  fun getExportTar(task: Task): ByteArray {
    val out = ByteArrayOutputStream()

    TarUtil.PosixTarArchiveOutputStream(out).use {
      copyTaskFiles(task, it)
      writeTaskDefinition(task, it)
    }

    return out.toByteArray()
  }

  private fun copyTaskFiles(task: Task, tar: TarUtil.PosixTarArchiveOutputStream) {
    fileService.readCollectionTar(task.id).use { files ->
      TarUtil.copyEntries(
          TarArchiveInputStream(files),
          tar,
          filter = { !TarUtil.isRoot(it) && !TarUtil.isCodefreakDefinition(it) }
      )
    }
  }

  private fun writeTaskDefinition(task: Task, tar: TarArchiveOutputStream) {
    val definition = task.toYamlDefinition().let { yamlMapper.writeValueAsBytes(it) }
    writeArchiveEntry(tar, TarUtil.CODEFREAK_DEFINITION_YML, definition)
  }

  private fun writeArchiveEntry(tar: TarArchiveOutputStream, entryName: String, content: ByteArray) {
    val entry = TarArchiveEntry(entryName).also { it.size = content.size.toLong() }

    tar.putArchiveEntry(entry)
    tar.write(content)
    tar.closeArchiveEntry()
  }

  /**
   * Creates a tar archive containing the given tasks each as a tar archive.
   *
   * @param tasks the tasks to be exported
   * @return a tar containing the exported tasks
   */
  @Transactional(readOnly = true)
  fun getExportTar(tasks: Collection<Task>): ByteArray {
    val out = ByteArrayOutputStream()

    TarUtil.PosixTarArchiveOutputStream(out).use {
      tasks.forEach { task ->
        // use findTask() on each task so everything is lazy initialized correctly
        val content = getExportTar(taskService.findTask(task.id))

        // Use a combination of the task title and its id so tasks with the same name don't overwrite each other
        val entryName = "${task.title}-${task.id}.tar"

        writeArchiveEntry(it, entryName, content)
      }
    }

    return out.toByteArray()
  }
}
