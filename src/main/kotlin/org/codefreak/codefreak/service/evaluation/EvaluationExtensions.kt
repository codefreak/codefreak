package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.service.evaluation.runner.CommentRunner

fun EvaluationRunner.isBuiltIn() = getName() == CommentRunner.RUNNER_NAME
