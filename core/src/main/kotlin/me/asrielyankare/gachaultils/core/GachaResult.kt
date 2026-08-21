package me.asrielyankare.gachaultils.core

/**
 * Typed result type for all operations.
 * Replaces generic Exception catching with explicit error handling.
 */
sealed class GachaResult<out T> {
    data class Success<T>(val data: T) : GachaResult<T>()
    data class Failure(val error: GachaError) : GachaResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw GachaException(error)
    }

    fun <R> map(transform: (T) -> R): GachaResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    fun <R> flatMap(transform: (T) -> GachaResult<R>): GachaResult<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this
    }

    companion object {
        fun <T> success(data: T): GachaResult<T> = Success(data)
        fun <T> failure(error: GachaError): GachaResult<T> = Failure(error)
        fun <T> failure(message: String): GachaResult<T> = Failure(GachaError.Unknown(message))
        fun <T> runCatching(block: () -> T): GachaResult<T> = try {
            Success(block())
        } catch (e: GachaException) {
            Failure(e.error)
        } catch (e: Exception) {
            Failure(GachaError.Unknown(e.message ?: "Unknown error"))
        }
    }
}

/**
 * Typed error hierarchy for the application.
 */
sealed class GachaError : Exception() {
    abstract override val message: String
    abstract override val cause: Throwable?

    data class BlackBoxInitializationError(
        override val message: String = "Failed to initialize NewBlackbox",
        override val cause: Throwable? = null
    ) : GachaError()

    data class PackageInstallError(
        val packageName: String,
        val reason: String,
        override val message: String = "Failed to install package: $packageName - $reason",
        override val cause: Throwable? = null
    ) : GachaError()

    data class LaunchError(
        val packageName: String,
        val userId: Int,
        override val message: String = "Failed to launch $packageName for user $userId",
        override val cause: Throwable? = null
    ) : GachaError()

    data class InstanceNotFound(
        val instanceId: Int,
        override val message: String = "Instance $instanceId not found",
        override val cause: Throwable? = null
    ) : GachaError()

    data class SaveNotFound(
        val instanceId: Int,
        val packageName: String,
        override val message: String = "No saves found for instance $instanceId ($packageName)",
        override val cause: Throwable? = null
    ) : GachaError()

    data class SaveAccessError(
        val path: String,
        override val message: String = "Cannot access save at: $path",
        override val cause: Throwable? = null
    ) : GachaError()

    data class SnapshotCorrupted(
        val fileName: String,
        val expectedHash: String,
        val actualHash: String,
        override val message: String = "Snapshot $fileName corrupted: expected $expectedHash, got $actualHash",
        override val cause: Throwable? = null
    ) : GachaError()

    data class RestoreVerificationFailed(
        val fileName: String,
        val expectedHash: String,
        val actualHash: String,
        override val message: String = "Restore verification failed for $fileName: expected $expectedHash, got $actualHash",
        override val cause: Throwable? = null
    ) : GachaError()

    data class OperationInProgress(
        val instanceId: Int,
        val operation: String,
        override val message: String = "Operation '$operation' is already in progress for instance $instanceId",
        override val cause: Throwable? = null
    ) : GachaError()

    data class InvalidState(
        val currentState: InstanceState,
        val attemptedOperation: String,
        override val message: String = "Cannot perform '$attemptedOperation' in state $currentState",
        override val cause: Throwable? = null
    ) : GachaError()

    data class ApkValidationError(
        val reason: String,
        override val message: String = "APK validation failed: $reason",
        override val cause: Throwable? = null
    ) : GachaError()

    data class UserCreationError(
        val userId: Int,
        override val message: String = "Failed to create user $userId in NewBlackbox",
        override val cause: Throwable? = null
    ) : GachaError()

    data class StorageError(
        val path: String,
        override val message: String = "Storage error at: $path",
        override val cause: Throwable? = null
    ) : GachaError()

    data class Unknown(
        override val message: String,
        override val cause: Throwable? = null
    ) : GachaError()
}

/**
 * Exception wrapper for GachaError that can be thrown.
 */
class GachaException(val error: GachaError) : Exception(error.message, error.cause)
