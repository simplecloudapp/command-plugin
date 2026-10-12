package app.simplecloud.plugin.command.shared.utilities

import app.simplecloud.api.web.ApiException
import app.simplecloud.api.web.models.ModelsErrorResponse
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeoutException

object ApiUtils {

    private val logger = LoggerFactory.getLogger(ApiUtils::class.java)

    fun reason(throwable: Throwable): String {
        val causes = generateSequence(throwable) { it.cause }
        val apiException = causes.filterIsInstance<ApiException>().firstOrNull()

        if (apiException == null) {
            if (causes.any { it is TimeoutException }) return "the controller did not respond in time"
            logger.error("An unexpected error occurred while executing a command", throwable)
            return "an internal error occurred"
        }
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
