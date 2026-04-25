package app.simplecloud.plugin.command.shared.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.command.shared.CloudSender
import app.simplecloud.plugin.command.shared.CommandPermission
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.shared.tags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.permission.Permission
import org.incendo.cloud.suggestion.Suggestion

class ServerCommand(
    private val api: CloudApi,
    private val plugin: CommandPlugin
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun <C : CloudSender> register(commandManager: CommandManager<C>) {
        registerList(commandManager)
        registerInfo(commandManager)
        registerStart(commandManager)
        registerStop(commandManager)
    }

    private fun <C : CloudSender> registerList(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("server")
                .literal("list")
                .optional("group", stringParser()) { _, _ ->
                    api.group().allGroups.thenApply { groups ->
                        groups.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .handler { context ->
                    val groupName = context.getOrDefault("group", null as String?)
                    val messages = plugin.messageConfiguration
                    scope.launch {
                        try {
                            val servers = if (groupName == null) {
                                api.server().allServers.await()
                            } else {
                                api.server().getServersByGroup(groupName).await()
                            }
                            if (servers.isEmpty()) {
                                context.sender().sendMessage(messages.msg(messages.command.server.list.empty))
                                return@launch
                            }
                            context.sender().sendMessage(
                                messages.msg(messages.command.server.list.title, tags("count" to servers.size))
                            )
                            servers.forEach { server ->
                                val serverName = "${server.group?.name ?: server.serverGroupId} ${server.numericalId}"
                                val value = "${server.playerCount ?: 0}/${server.maxPlayers} - ${server.state.name.lowercase()}"
                                context.sender().sendMessage(
                                    messages.msg(messages.command.server.list.entry, tags("server" to serverName, "value" to value))
                                )
                            }
                        } catch (_: Exception) {
                            context.sender().sendMessage(messages.msg(messages.command.error.cloudUnavailable))
                        }
                    }
                }
                .permission(Permission.permission(CommandPermission.SERVER_LIST))
                .build()
        )
    }

    private fun <C : CloudSender> registerInfo(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("server")
                .literal("info")
                .required("group", stringParser()) { _, _ ->
                    api.group().allGroups.thenApply { groups ->
                        groups.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .required("id", integerParser()) { context, _ ->
                    val group = context.get<String>("group")
                    api.server().getServersByGroup(group).thenApply { servers ->
                        servers.map { Suggestion.suggestion(it.numericalId.toString()) }
                    }.exceptionally { emptyList() }
                }
                .handler { context ->
                    val groupName = context.get<String>("group")
                    val id = context.get<Int>("id")
                    val messages = plugin.messageConfiguration
                    scope.launch {
                        try {
                            val server = api.server().getServerByNumericalId(groupName, id).await()
                            if (server == null) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.server.error.notFound, tags("group" to groupName, "id" to id))
                                )
                                return@launch
                            }
                            val serverName = "${groupName} $id"
                            context.sender().sendMessage(
                                messages.msg(messages.command.server.info.title, tags("server" to serverName))
                            )
                            context.sender().sendMessage(
                                messages.msg(messages.command.server.info.entry, tags("key" to "State", "value" to server.state.name))
                            )
                            context.sender().sendMessage(
                                messages.msg(
                                    messages.command.server.info.entry,
                                    tags("key" to "Memory", "value" to "${server.minMemory}-${server.maxMemory} MB")
                                )
                            )
                            context.sender().sendMessage(
                                messages.msg(
                                    messages.command.server.info.entry,
                                    tags("key" to "Players", "value" to "${server.playerCount ?: 0}/${server.maxPlayers}")
                                )
                            )
                        } catch (_: Exception) {
                            context.sender().sendMessage(messages.msg(messages.command.error.internal))
                        }
                    }
                }
                .permission(Permission.permission(CommandPermission.SERVER_INFO))
                .build()
        )
    }

    private fun <C : CloudSender> registerStart(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("server")
                .literal("start")
                .required("group", stringParser()) { _, _ ->
                    api.group().allGroups.thenApply { groups ->
                        groups.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .handler { context ->
                    val groupName = context.get<String>("group")
                    val messages = plugin.messageConfiguration
                    scope.launch {
                        try {
                            val group = api.group().getGroupByName(groupName).await()
                            if (group == null) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.group.error.notFound, tags("group" to groupName))
                                )
                                return@launch
                            }
                            try {
                                val server = api.group().requestServerStart(group.serverGroupId).await()
                                context.sender().sendMessage(
                                    messages.msg(messages.command.server.start.success, tags("group" to group.name))
                                )
                            } catch (_: Exception) {
                                api.group().requestServerStart(group).await()
                                context.sender().sendMessage(
                                    messages.msg(messages.command.server.start.queued, tags("group" to group.name, "id" to "?"))
                                )
                            }
                        } catch (_: Exception) {
                            context.sender().sendMessage(
                                messages.msg(messages.command.server.error.startFailed, tags("group" to groupName, "id" to "?"))
                            )
                        }
                    }
                }
                .permission(Permission.permission(CommandPermission.SERVER_START))
                .build()
        )
    }

    private fun <C : CloudSender> registerStop(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("server")
                .literal("stop")
                .required("group", stringParser()) { _, _ ->
                    api.group().allGroups.thenApply { groups ->
                        groups.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .required("id", integerParser()) { context, _ ->
                    val group = context.get<String>("group")
                    api.server().getServersByGroup(group).thenApply { servers ->
                        servers.map { Suggestion.suggestion(it.numericalId.toString()) }
                    }.exceptionally { emptyList() }
                }
                .handler { context ->
                    val groupName = context.get<String>("group")
                    val id = context.get<Int>("id")
                    val messages = plugin.messageConfiguration
                    scope.launch {
                        try {
                            val server = api.server().getServerByNumericalId(groupName, id).await()
                            if (server == null) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.server.error.notFound, tags("group" to groupName, "id" to id))
                                )
                                return@launch
                            }
                            if (server.state.name.equals("STOPPING", true)) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.server.error.alreadyStopped, tags("group" to groupName, "id" to id))
                                )
                                return@launch
                            }
                            api.server().stopServer(server.serverId).await()
                            context.sender().sendMessage(
                                messages.msg(messages.command.server.stop.success, tags("group" to groupName, "id" to id))
                            )
                        } catch (_: Exception) {
                            context.sender().sendMessage(
                                messages.msg(messages.command.server.error.stopFailed, tags("group" to groupName, "id" to id))
                            )
                        }
                    }
                }
                .permission(Permission.permission(CommandPermission.SERVER_STOP))
                .build()
        )
    }
}
