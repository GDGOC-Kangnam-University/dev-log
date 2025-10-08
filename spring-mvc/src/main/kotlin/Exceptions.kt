package org.example

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class DatabaseOperationError : Exception()

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundError : Exception()

@ResponseStatus(HttpStatus.FORBIDDEN)
class ForbiddenError : Exception()

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthorizedError : Exception()