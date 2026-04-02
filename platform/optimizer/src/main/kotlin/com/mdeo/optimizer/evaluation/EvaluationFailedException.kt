package com.mdeo.optimizer.evaluation

/**
 * Thrown when a guidance function (objective or constraint) evaluation fails during optimization.
 *
 * Unlike mutation failures (which are silently penalized), an evaluation failure indicates
 * that a user-defined script threw an exception and the optimization cannot proceed reliably.
 *
 * @param message Description of the failure, typically including the original exception message.
 */
class EvaluationFailedException(message: String) : RuntimeException(message)
