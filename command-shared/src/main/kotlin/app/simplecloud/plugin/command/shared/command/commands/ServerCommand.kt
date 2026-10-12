package app.simplecloud.plugin.command.shared.command.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import app.simplecloud.plugin.command.shared.command.CloudSender
import app.simplecloud.plugin.command.shared.utilities.CommandPermissions
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.shared.command.CloudSuggestions
import app.simplecloud.plugin.command.shared.utilities.ApiUtils
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
                .suspendingHandler(plugin.scope) { context ->
                    val sender = context.sender()
                    val groupName = context.getOrDefault("group", null as String?)
                    val messages = plugin.messageConfiguration
                    val servers = if (groupName == null) {
                        api.server().allServers.await()
                    } else {
                        val group = api.group().allGroups.await().firstOrNull { it.name.equals(groupName, true) }
                        if (group == null) {
                            sender.sendMessage(messages.msg(messages.command.group.error.notFound, tags("group" to groupName)))
                            return@suspendingHandler
                        }
                        api.server().getServersByGroup(group.name).await()
                    }
                    if (servers.isEmpty()) {
                        sender.sendMessage(messages.msg(messages.command.server.list.empty))
                        return@suspendingHandler
                    }
                    sender.sendMessage(messages.msg(messages.command.server.list.title, tags("count" to servers.size)))
                    servers.forEach { server ->
                        val serverName = displayName(server)
                        val value = "${server.playerCount ?: 0}/${server.maxPlayers} - ${server.state.name.lowercase()}"
                        sender.sendMessage(messages.msg(messages.command.server.list.entry, tags("server" to serverName, "value" to value)))
                    }
                }
                .permission(Permission.permission(CommandPermissions.SERVER_LIST))
                .build()
        )
    }

    private fun displayName(server: Server): String {
        val groupName = server.group?.name
        val persistentServerName = server.persistentServer?.name
        return when {
            !groupName.isNullOrBlank() -> "$groupName ${server.numericalId}"
            !server.serverGroupId.isNullOrBlank() -> "${server.serverGroupId} ${server.numericalId}"
            !persistentServerName.isNullOrBlank() -> persistentServerName
            !server.persistentServerId.isNullOrBlank() -> server.persistentServerId
            else -> server.serverId
        }
    }

    private fun registerInfo() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("server")
                .literal("info")
                .required("group", stringParser(), CloudSuggestions.groups(api))
                .required("id", integerParser(1), CloudSuggestions.serverIds(api))
                .suspendingHandler(plugin.scope) { context ->
                    val sender = context.sender()
                    val groupName = context.get<String>("group")
                    val id = context.get<Int>("id")
                    val messages = plugin.messageConfiguration
                    val group = api.group().allGroups.await().firstOrNull { it.name.equals(groupName, true) }
                    if (group == null) {
                        sender.sendMessage(messages.msg(messages.command.group.error.notFound, tags("group" to groupName)))
                        return@suspendingHandler
                    }
                    val server = api.server().getServersByGroup(group.name).await().firstOrNull { it.numericalId == id }
                    if (server == null) {
                        sender.sendMessage(messages.msg(messages.command.server.error.notFound, tags("group" to group.name, "id" to id)))
                        return@suspendingHandler
                    }
                    val serverName = "${group.name} $id"
                    sender.sendMessage(messages.msg(messages.command.server.info.title, tags("server" to serverName)))
                    sender.sendMessage(messages.msg(messages.command.server.info.entry, tags("key" to "State", "value" to server.state.name)))
                    sender.sendMessage(messages.msg(messages.command.server.info.entry, tags("key" to "Memory", "value" to "${server.minMemory}-${server.maxMemory} MB")))
                    sender.sendMessage(messages.msg(messages.command.server.info.entry, tags("key" to "Players", "value" to "${server.playerCount ?: 0}/${server.maxPlayers}")))
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
                .suspendingHandler(plugin.scope) { context ->
                    val sender = context.sender()
                    val groupName = context.get<String>("group")
                    val messages = plugin.messageConfiguration
                    val group = api.group().allGroups.await().firstOrNull { it.name.equals(groupName, true) }
                    if (group == null) {
                        sender.sendMessage(messages.msg(messages.command.group.error.notFound, tags("group" to groupName)))
                        return@suspendingHandler
                    }
                    runCatching { api.group().requestServerStart(group).await() }
                        .onSuccess { sender.sendMessage(messages.msg(messages.command.server.start.requested, tags("group" to group.name))) }
                        .onFailure {
                            sender.sendMessage(messages.msg(
                                messages.command.server.error.startFailed,
                                tags("group" to group.name, "reason" to ApiUtils.reason(it))
                            ))
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
                .required("id", integerParser(1), CloudSuggestions.serverIds(api))
                .suspendingHandler(plugin.scope) { context ->
                    val sender = context.sender()
                    val groupName = context.get<String>("group")
                    val id = context.get<Int>("id")
                    val messages = plugin.messageConfiguration
                    val group = api.group().allGroups.await().firstOrNull { it.name.equals(groupName, true) }
                    if (group == null) {
                        sender.sendMessage(messages.msg(messages.command.group.error.notFound, tags("group" to groupName)))
                        return@suspendingHandler
                    }
                    val server = api.server().getServersByGroup(group.name).await().firstOrNull { it.numericalId == id }
                    if (server == null) {
                        sender.sendMessage(messages.msg(messages.command.server.error.notFound, tags("group" to group.name, "id" to id)))
                        return@suspendingHandler
                    }
                    if (server.state == ServerState.STOPPING || server.state == ServerState.CLEANUP) {
                        sender.sendMessage(messages.msg(messages.command.server.error.alreadyStopping, tags("group" to group.name, "id" to id)))
                        return@suspendingHandler
                    }
                    runCatching { api.server().stopServer(server.serverId).await() }
                        .onSuccess { sender.sendMessage(messages.msg(messages.command.server.stop.success, tags("group" to group.name, "id" to id))) }
                        .onFailure {
                            sender.sendMessage(messages.msg(
                                messages.command.server.error.stopFailed,
                                tags("group" to group.name, "id" to id, "reason" to ApiUtils.reason(it))
                            ))
                        }
                }
                .permission(Permission.permission(CommandPermissions.SERVER_STOP))
                .build()
        )
    }
}
