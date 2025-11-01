package org.example;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class DatabaseOperationError extends RuntimeException {
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundError extends RuntimeException {
}

@ResponseStatus(HttpStatus.FORBIDDEN)
class ForbiddenError extends RuntimeException {
}

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthorizedError extends RuntimeException {
}
