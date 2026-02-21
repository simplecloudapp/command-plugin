package app.simplecloud.plugin.command.shared.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.command.shared.CloudSender
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.shared.miniMessage
import app.simplecloud.plugin.command.shared.resolver.PersistentServerTagResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.permission.Permission
import org.incendo.cloud.suggestion.Suggestion

class StartCommand(
    private val api: CloudApi,
    private val plugin: CommandPlugin
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun <C : CloudSender> register(commandManager: CommandManager<C>) {
//        commandManager.command(
//            commandManager.commandBuilder("cloud")
//                .literal("start")
//                .literal("group")
//                .required("group", stringParser()) { _, _ ->
//                    api.group().allGroups
//                        .thenApply { groups ->
//                            groups.map { Suggestion.suggestion(it.name) }
//                        }
//                        .exceptionally { emptyList() }
//                }
//                .handler { context ->
//                    val groupName = context.get<String>("group")
//                    scope.launch {
//                        try {
//                            val group = api.group().getGroupByName(groupName).await()
//                            api.server().startServer(StartServerRequest(group.serverGroupId, group.name))
//                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.serverStarting, GroupTagResolver.of(group)))
//                        } catch (e: Exception) {
//                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.errorMessage, Placeholder.unparsed("error", e.message ?: "Unknown error")))
//                        }
//                    }
//                }
//                .permission(Permission.permission("simplecloud.command.cloud.start.group"))
//                .build()
//        )

        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("start")
                .literal("persistent")
                .required("server", stringParser()) { _, _ ->
                    api.persistentServer().allPersistentServers
                        .thenApply { servers ->
                            servers.map { Suggestion.suggestion(it.name) }
                        }
                        .exceptionally { emptyList() }
                }
                .handler { context ->
                    val serverName = context.get<String>("server")
                    scope.launch {
                        try {
                            val persistent = api.persistentServer().getPersistentServerByName(serverName).await()
                            api.persistentServer().updatePersistentServer(
                                persistent.persistentServerId,
                                EditCommand.persistentUpdateBuilder(persistent)
                                    .active(true)
                                    .build()
                            ).await()
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.persistentServerActivated, PersistentServerTagResolver.of(persistent)))
                        } catch (e: Exception) {
                            context.sender().sendMessage(
                                miniMessage(
                                    plugin.messageConfiguration.errorMessage,
                                    Placeholder.unparsed("error", e.message ?: "Unknown error")
                                )
                            )
                        }
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.start.persistent"))
                .build()
        )
    }
}