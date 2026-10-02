package app.simplecloud.plugin.command.shared.utilities

import app.simplecloud.api.web.ApiException
import app.simplecloud.api.web.models.ModelsErrorResponse

object ApiUtils {

    fun reason(throwable: Throwable): String {
        val apiException = generateSequence(throwable) { it.cause }
            .filterIsInstance<ApiException>()
            .firstOrNull()

        if (apiException == null) return "Internal error"
        if (apiException.code == 0) return "Controller is not reachable"
        return "${parseErrorMessage(apiException.responseBody)} (HTTP ${apiException.code})"
    }

    private fun parseErrorMessage(body: String?): String {
        if (body.isNullOrBlank()) return "Unknown error"
        return try {
            ModelsErrorResponse.fromJson(body).error ?: "Unknown error"
        } catch (_: Exception) {
            "Unknown error"
        }
    }
}
