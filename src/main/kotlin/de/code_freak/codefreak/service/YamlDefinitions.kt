package de.code_freak.codefreak.service

data class TaskDefinition(
  val title: String,
  val description: String? = null,
  val hidden: List<String> = emptyList(),
  val protected: List<String> = emptyList(),
  val evaluation: List<EvaluationDefinition> = emptyList()
) {
  private constructor() : this("")
}

data class EvaluationDefinition(
  val step: String,
  val options: Map<String, Any> = emptyMap()
) {
  private constructor() : this("")
}
