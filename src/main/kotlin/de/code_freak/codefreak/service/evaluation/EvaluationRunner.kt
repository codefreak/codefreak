package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.entity.Answer
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

interface EvaluationRunner {

  fun getName(): String
  fun run(answer: Answer, options: Map<String, Any>): String
  fun parseResultContent(content: ByteArray): Any
  fun getResultState(parsedResultContent: Any): ResultType

  fun <T : Any> Map<String, Any>.get(key: String, type: KClass<T>): T? =
      get(key)?.let { type.safeCast(it) ?: throw IllegalArgumentException("Option '$key' has invalid format") }

  fun <T : Any> Map<String, Any>.getRequired(key: String, type: KClass<T>): T =
      get(key, type) ?: throw IllegalArgumentException("Option '$key' is required")

  fun <T : Any> Map<String, Any>.getList(key: String, type: KClass<T>, required: Boolean = false): List<T>? {
    val result = if (required) getRequired(key, List::class) else get(key, List::class)
    return result?.map { type.safeCast(it) ?: throw IllegalArgumentException("Option '$key' has invalid format") }
  }
}
