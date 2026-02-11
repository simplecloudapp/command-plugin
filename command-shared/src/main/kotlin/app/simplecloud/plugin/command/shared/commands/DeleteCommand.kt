package app.simplecloud.plugin.command.shared.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.command.shared.CloudSender
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.shared.miniMessage
import app.simplecloud.plugin.command.shared.resolver.GroupTagResolver
import app.simplecloud.plugin.command.shared.resolver.PersistentServerTagResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.permission.Permission
import org.incendo.cloud.suggestion.Suggestion

class DeleteCommand(
    private val api: CloudApi,
    private val plugin: CommandPlugin
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun <C : CloudSender> register(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("delete")
                .literal("group")
                .required("group", stringParser()) { _, _ ->
                    api.group().allGroups.thenApply { groups ->
                        groups.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .handler { context ->
                    val groupName = context.get<String>("group")
                    scope.launch {
                        try {
                            val group = api.group().getGroupByName(groupName).await()
                            val servers = api.server().getServersByGroup(groupName).await()
                            servers.forEach {
                                try {
                                    api.server().stopServer(it.serverId).await()
                                } catch (_: Exception) {
                                }
                            }
                            repeat(30) {
                                val remaining = api.server().getServersByGroup(groupName).await()
                                if (remaining.isEmpty()) return@repeat
                                delay(1000)
                            }
                            api.group().deleteGroup(group.serverGroupId).await()
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.groupDeleted, GroupTagResolver.of(group)))
                        } catch (e: Exception) {
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.errorMessage, Placeholder.unparsed("error", e.message ?: "Unknown error")))
                        }
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.delete.group"))
                .build()
        )

        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("delete")
                .literal("persistent")
                .required("server", stringParser()) { _, _ ->
                    api.persistentServer().allPersistentServers.thenApply { servers ->
                        servers.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .handler { context ->
                    val serverName = context.get<String>("server")
                    scope.launch {
                        try {
                            val persistent = api.persistentServer().getPersistentServerByName(serverName).await()
                            api.persistentServer().deletePersistentServer(persistent.persistentServerId).await()
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.persistentServerDeleted, PersistentServerTagResolver.of(persistent)))
                        } catch (e: Exception) {
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.errorMessage, Placeholder.unparsed("error", e.message ?: "Unknown error")))
                        }
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.delete.persistent"))
                .build()
        )
    }
}
