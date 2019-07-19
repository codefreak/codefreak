package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.service.EntityNotFoundException
import de.code_freak.codefreak.service.LatexService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.util.UUID

/**
 * Global exception handler for the frontend (is applied to all controllers).
 */
@ControllerAdvice
class ExceptionHandlerAdvice : ResponseEntityExceptionHandler() {

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(throwable: Throwable, controllerMethod: HandlerMethod): Any {
    return getResponse(throwable.message, controllerMethod, HttpStatus.NOT_FOUND)
  }

  @ExceptionHandler(LatexService.CompilerException::class)
  fun handleLatexCompilerExceptions(
    ex: LatexService.CompilerException,
    controllerMethod: HandlerMethod
  ): Any {
    return getResponse("PDF download is currently not available", controllerMethod, HttpStatus.SERVICE_UNAVAILABLE)
  }

  @ExceptionHandler(IllegalArgumentException::class)
  fun handleIllegalArgumentException(
    ex: IllegalArgumentException,
    controllerMethod: HandlerMethod
  ): Any {
    return getResponse(ex.message, controllerMethod, HttpStatus.BAD_REQUEST)
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(
    ex: MethodArgumentTypeMismatchException,
    controllerMethod: HandlerMethod
  ): Any {
    return if (ex.parameter.parameterType == UUID::class.java) {
      getResponse(null, controllerMethod, HttpStatus.NOT_FOUND)
    } else {
      getResponse(ex.message, controllerMethod, HttpStatus.BAD_REQUEST)
    }
  }

  /**
   * Generates a response depending on the current controller. For normal controllers the "error"
   * template is returned and supplied with the usual model, for REST controllers the
   * `message` is returned as response body.
   */
  protected fun getResponse(message: Any?, controllerMethod: HandlerMethod, status: HttpStatus): Any {

    val isRestController = (controllerMethod.hasMethodAnnotation(ResponseBody::class.java) ||
        controllerMethod.beanType.isAnnotationPresent(ResponseBody::class.java) ||
        controllerMethod.beanType.isAnnotationPresent(RestController::class.java))

    return if (isRestController) {
      ResponseEntity(message, status)
    } else {
      val model = mapOf(
          "message" to (message ?: "No message available"),
          "status" to status.value(),
          "error" to status.reasonPhrase
      )
      ModelAndView("error", model, status)
    }
  }
}
