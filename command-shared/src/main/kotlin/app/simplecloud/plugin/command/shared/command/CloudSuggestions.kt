package app.simplecloud.plugin.command.shared.command

import app.simplecloud.api.CloudApi
import kotlinx.coroutines.future.await
import org.incendo.cloud.kotlin.coroutines.SuspendingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider

object CloudSuggestions {

    fun <C : CloudSender> groups(api: CloudApi): SuggestionProvider<C> =
        SuspendingSuggestionProvider<C> { _, _ ->
            try {
                api.group().allGroups.await().map { Suggestion.suggestion(it.name) }
            } catch (_: Exception) {
                emptyList()
            }
        }.asSuggestionProvider()

    fun <C : CloudSender> serverIds(api: CloudApi, arg: String = "group"): SuggestionProvider<C> =
        SuspendingSuggestionProvider<C> { context, _ ->
            try {
                val group = context.get<String>(arg)
                api.server().getServersByGroup(group).await().map { Suggestion.suggestion(it.numericalId.toString()) }
            } catch (_: Exception) {
                emptyList()
            }
        }.asSuggestionProvider()

    fun <C : CloudSender> onlinePlayers(api: CloudApi): SuggestionProvider<C> =
        SuspendingSuggestionProvider<C> { _, _ ->
            try {
                api.player().onlinePlayers.await().map { Suggestion.suggestion(it.name) }
            } catch (_: Exception) {
                emptyList()
            }
        }.asSuggestionProvider()

    fun <C : CloudSender> targetTypes(): SuggestionProvider<C> =
        SuspendingSuggestionProvider<C> { _, _ ->
            listOf(Suggestion.suggestion("group"), Suggestion.suggestion("ps"))
        }.asSuggestionProvider()
}