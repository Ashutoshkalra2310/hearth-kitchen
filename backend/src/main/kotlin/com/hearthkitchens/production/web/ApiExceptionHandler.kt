package com.hearthkitchens.production.web

import com.hearthkitchens.production.service.InsufficientStockException
import com.hearthkitchens.production.service.InvalidOrderStatusException
import com.hearthkitchens.production.service.NotFoundException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Turns errors into RFC 9457 problem details:
 * { "status": 409, "title": "...", "detail": "...", ...extra properties }
 * The base class already covers malformed JSON and bad path/query values (400).
 */
@RestControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, "Not found", ex.message)

    @ExceptionHandler(InvalidOrderStatusException::class)
    fun handleInvalidStatus(ex: InvalidOrderStatusException): ProblemDetail =
        problem(HttpStatus.CONFLICT, "Invalid order status", ex.message)

    /** Release rejected: the body lists every short ingredient. */
    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(ex: InsufficientStockException): ProblemDetail =
        problem(HttpStatus.CONFLICT, "Insufficient stock", ex.message).apply {
            setProperty("shortages", ex.shortages)
        }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "Bad request", ex.message)

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val detail = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field} ${it.defaultMessage}" }
        return handleExceptionInternal(ex, problem(HttpStatus.BAD_REQUEST, "Bad request", detail), headers, status, request)
    }

    private fun problem(status: HttpStatus, title: String, detail: String?): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail ?: title).apply { this.title = title }
}
