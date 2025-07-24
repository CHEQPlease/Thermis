package com.cheqplease.thermis

/**
 * Sealed class representing the result of a print operation
 */
sealed class PrintResult {
    object Success : PrintResult()
    data class Failed(val reason: PrintFailureReason, val retryable: Boolean, val message: String? = null) : PrintResult()
    
    fun isSuccess(): Boolean = this is Success
    fun isFailed(): Boolean = this is Failed
    
    fun toMap(): Map<String, Any?> {
        return when (this) {
            is Success -> mapOf(
                "success" to true,
                "reason" to null,
                "retryable" to false,
                "message" to null
            )
            is Failed -> mapOf(
                "success" to false,
                "reason" to reason.name,
                "retryable" to retryable,
                "message" to message
            )
        }
    }
}

/**
 * Enum representing different types of print failures
 */
enum class PrintFailureReason {
    PRINTER_BUSY,
    PRINTER_OFFLINE,
    PRINTER_NOT_FOUND,
    OUT_OF_PAPER,
    COVER_OPEN,
    NETWORK_ERROR,
    COMMUNICATION_ERROR,
    DEVICE_IN_USE,
    TIMEOUT_ERROR,
    UNKNOWN_ERROR;
    
    fun isRetryable(): Boolean {
        return when (this) {
            PRINTER_BUSY,
            DEVICE_IN_USE,
            NETWORK_ERROR,
            COMMUNICATION_ERROR -> true
            TIMEOUT_ERROR -> true
            PRINTER_OFFLINE,
            PRINTER_NOT_FOUND,
            OUT_OF_PAPER,
            COVER_OPEN -> true
            UNKNOWN_ERROR -> false
        }
    }
} 