package app.simplecloud.plugin.command.shared.command.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.api.server.Server
import app.simplecloud.plugin.command.shared.command.CloudSender
import app.simplecloud.plugin.command.shared.utilities.CommandPermissions
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.shared.command.CloudSuggestions
import app.simplecloud.plugin.command.shared.utilities.tags
import kotlinx.coroutines.future.await
import org.incendo.cloud.CommandManager
import org.incendo.cloud.kotlin.coroutines.extension.suspendingHandler
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.permission.Permission

class ServerCommand<C : CloudSender>(
    private val api: CloudApi,
    private val plugin: CommandPlugin,
    private val manager: CommandManager<C>
) {

    fun register() {
        registerList()
        registerInfo()
        registerStart()
        registerStop()
    }

    private fun registerList() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("server")
                .literal("list")
                .optional("group", stringParser(), CloudSuggestions.groups(api))
                .suspendingHandler { context ->
                    val sender = context.sender()
                    val groupName = context.getOrDefault("group", null as String?)
                    val messages = plugin.messageConfiguration
                    try {
                        val servers = if (groupName == null) {
                            api.server().allServers.await()
                        } else {
                            api.server().getServersByGroup(groupName).await()
                        }
                        if (servers.isEmpty()) {
                            sender.sendMessage(messages.msg(messages.command.server.list.empty))
                            return@suspendingHandler
                        }
                        sender.sendMessage(messages.msg(messages.command.server.list.title, tags("count" to servers.size)))
                        servers.forEach { server ->
                            val serverName = server.displayName()
                            val value = "${server.playerCount ?: 0}/${server.maxPlayers} - ${server.state.name.lowercase()}"
                            sender.sendMessage(messages.msg(messages.command.server.list.entry, tags("server" to serverName, "value" to value)))
                        }
                    } catch (_: Exception) {
                        sender.sendMessage(messages.msg(messages.command.error.cloudUnavailable))
                    }
                }
                .permission(Permission.permission(CommandPermissions.SERVER_LIST))
                .build()
        )
    }

    private fun Server.displayName(): String {
        val groupName = group?.name?.takeUnless { it.isBlank() }
            ?: serverGroupId?.takeUnless { it.isBlank() }
        if (groupName != null) {
            return "$groupName $numericalId"
        }

        return persistentServer?.name?.takeUnless { it.isBlank() }
            ?: persistentServerId?.takeUnless { it.isBlank() }
            ?: serverId
    }

    private fun registerInfo() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("server")
                .literal("info")
                .required("group", stringParser(), CloudSuggestions.groups(api))
                .required("id", integerParser(), CloudSuggestions.serverIds(api))
                .suspendingHandler { context ->
                    val sender = context.sender()
                    val groupName = context.get<String>("group")
                    val id = context.get<Int>("id")
                    val messages = plugin.messageConfiguration
                    try {
                        val server = api.server().getServerByNumericalId(groupName, id).await()
                        if (server == null) {
                            sender.sendMessage(messages.msg(messages.command.server.error.notFound, tags("group" to groupName, "id" to id)))
                            return@suspendingHandler
                        }
                        val serverName = "$groupName $id"
                        sender.sendMessage(messages.msg(messages.command.server.info.title, tags("server" to serverName)))
                        sender.sendMessage(messages.msg(messages.command.server.info.entry, tags("key" to "State", "value" to server.state.name)))
                        sender.sendMessage(messages.msg(messages.command.server.info.entry, tags("key" to "Memory", "value" to "${server.minMemory}-${server.maxMemory} MB")))
                        sender.sendMessage(messages.msg(messages.command.server.info.entry, tags("key" to "Players", "value" to "${server.playerCount ?: 0}/${server.maxPlayers}")))
                    } catch (_: Exception) {
                        sender.sendMessage(messages.msg(messages.command.error.internal))
                    }
                }
                .permission(Permission.permission(CommandPermissions.SERVER_INFO))
                .build()
        )
    }

    private fun registerStart() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("server")
                .literal("start")
                .required("group", stringParser(), CloudSuggestions.groups(api))
                .suspendingHandler { context ->
                    val sender = context.sender()
                    val groupName = context.get<String>("group")
                    val messages = plugin.messageConfiguration
                    try {
                        val group = api.group().getGroupByName(groupName).await()
                        if (group == null) {
                            sender.sendMessage(messages.msg(messages.command.group.error.notFound, tags("group" to groupName)))
                            return@suspendingHandler
                        }
                        try {
                            api.group().requestServerStart(group.serverGroupId).await()
                            sender.sendMessage(messages.msg(messages.command.server.start.success, tags("group" to group.name, "id" to "?")))
                        } catch (_: Exception) {
                            api.group().requestServerStart(group).await()
                            sender.sendMessage(messages.msg(messages.command.server.start.queued, tags("group" to group.name, "id" to "?")))
                        }
                    } catch (_: Exception) {
                        sender.sendMessage(messages.msg(messages.command.server.error.startFailed, tags("group" to groupName, "id" to "?")))
                    }
                }
                .permission(Permission.permission(CommandPermissions.SERVER_START))
                .build()
        )
    }

    private fun registerStop() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("server")
                .literal("stop")
                .required("group", stringParser(), CloudSuggestions.groups(api))
                .required("id", integerParser(), CloudSuggestions.serverIds(api))
                .suspendingHandler { context ->
                    val sender = context.sender()
                    val groupName = context.get<String>("group")
                    val id = context.get<Int>("id")
                    val messages = plugin.messageConfiguration
                    try {
                        val server = api.server().getServerByNumericalId(groupName, id).await()
                        if (server == null) {
                            sender.sendMessage(messages.msg(messages.command.server.error.notFound, tags("group" to groupName, "id" to id)))
                            return@suspendingHandler
                        }
                        if (server.state.name.equals("STOPPING", true)) {
                            sender.sendMessage(messages.msg(messages.command.server.error.alreadyStopped, tags("group" to groupName, "id" to id)))
                            return@suspendingHandler
                        }
                        api.server().stopServer(server.serverId).await()
                        sender.sendMessage(messages.msg(messages.command.server.stop.success, tags("group" to groupName, "id" to id)))
                    } catch (_: Exception) {
                        sender.sendMessage(messages.msg(messages.command.server.error.stopFailed, tags("group" to groupName, "id" to id)))
                    }
                }
                .permission(Permission.permission(CommandPermissions.SERVER_STOP))
                .build()
        )
    }
}