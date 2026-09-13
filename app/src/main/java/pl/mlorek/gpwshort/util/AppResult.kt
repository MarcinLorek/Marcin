package pl.mlorek.gpwshort.util

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val type: ErrorType, val cause: Throwable? = null) : AppResult<Nothing>
}

enum class ErrorType {
    NETWORK,
    PARSING,
    UNKNOWN,
}

inline fun <T> AppResult<T>.onSuccess(block: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) block(data)
    return this
}

inline fun <T> AppResult<T>.onError(block: (AppResult.Error) -> Unit): AppResult<T> {
    if (this is AppResult.Error) block(this)
    return this
}
