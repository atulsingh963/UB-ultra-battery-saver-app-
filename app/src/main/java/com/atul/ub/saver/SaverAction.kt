package com.atul.ub.saver

enum class CapabilityState {
    AVAILABLE,
    USER_PERMISSION_REQUIRED,
    UNSUPPORTED,
    FAILED
}

enum class ActionResultType {
    SUCCESS,
    SKIPPED,
    UNAVAILABLE,
    REQUIRES_PERMISSION,
    FAILED
}

data class SaverActionResult(
    val actionName: String,
    val description: String,
    val resultType: ActionResultType,
    val message: String,
    val settingsActionIntent: String? = null
)

interface SaverAction {
    val key: String
    val name: String
    val description: String
    val capability: CapabilityState

    fun isAlreadyActive(): Boolean
    fun execute(): SaverActionResult
    fun restore(previousValue: Any?): Boolean
    fun getCurrentValue(): Any?
}
