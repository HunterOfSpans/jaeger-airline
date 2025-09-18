package com.airline.reservation.common

/**
 * 함수형 에러 핸들링을 위한 Result 타입
 *
 * Kotlin의 관용구를 활용하여 예외 대신 Result로 성공/실패를 표현합니다.
 * 이를 통해 더 명시적이고 안전한 에러 핸들링이 가능합니다.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val error: DomainError) : Result<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw error.toException()
    }

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (DomainError) -> Unit): Result<T> {
        if (this is Failure) action(error)
        return this
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun <T> failure(error: DomainError): Result<T> = Failure(error)

        inline fun <T> runCatching(block: () -> T): Result<T> = try {
            Success(block())
        } catch (e: Exception) {
            Failure(DomainError.SystemError("Unexpected error: ${e.message}", e))
        }
    }
}

/**
 * 도메인별 에러 타입 정의
 */
sealed class DomainError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class FlightNotFound(
        val flightId: String,
        override val message: String = "Flight not found: $flightId"
    ) : DomainError(message)

    data class NoAvailableSeats(
        val flightId: String,
        override val message: String = "No available seats for flight: $flightId"
    ) : DomainError(message)

    data class PaymentDeclined(
        override val message: String,
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    data class TicketIssuanceFailed(
        override val message: String,
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    data class ValidationError(
        override val message: String
    ) : DomainError(message)

    data class SystemError(
        override val message: String,
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    fun toException(): RuntimeException = when (this) {
        is FlightNotFound -> RuntimeException(message)
        is NoAvailableSeats -> RuntimeException(message)
        is PaymentDeclined -> RuntimeException(message, cause)
        is TicketIssuanceFailed -> RuntimeException(message, cause)
        is ValidationError -> IllegalArgumentException(message)
        is SystemError -> RuntimeException(message, cause)
    }
}