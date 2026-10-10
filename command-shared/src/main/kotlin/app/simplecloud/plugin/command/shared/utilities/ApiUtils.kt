package app.simplecloud.plugin.command.shared.utilities

import app.simplecloud.api.web.ApiException
import app.simplecloud.api.web.models.ModelsErrorResponse

object ApiUtils {

    /**
     * Returns the reason of a failed api call.
     */
    fun reason(throwable: Throwable): String {
        val apiException = generateSequence(throwable) { it.cause }
            .filterIsInstance<ApiException>()
            .firstOrNull()

        if (apiException == null) return "an internal error occurred"
        if (apiException.code == 0) return "the controller is not reachable"
        return parseErrorMessage(apiException.responseBody)
    }

    private fun parseErrorMessage(body: String?): String {
        if (body.isNullOrBlank()) return "an unknown error occurred"
        val error = try {
            ModelsErrorResponse.fromJson(body).error
        } catch (_: Exception) {
            null
        }
        if (error.isNullOrBlank()) return "an unknown error occurred"

        // controller messages start uppercase and may end with a period, e.g. "Group not found."
        val message = error.trim().trimEnd('.')
        val isAcronym = message.length > 1 && message[1].isUpperCase()
        return if (isAcronym) message else message.replaceFirstChar { it.lowercase() }
    }
}
