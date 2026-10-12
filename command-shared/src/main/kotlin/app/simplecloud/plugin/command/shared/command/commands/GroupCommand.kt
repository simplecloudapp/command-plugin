package app.simplecloud.plugin.command.shared.command.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.api.server.ServerState
import app.simplecloud.plugin.command.shared.command.CloudSender
import app.simplecloud.plugin.command.shared.utilities.CommandPermissions
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.shared.command.CloudSuggestions
import app.simplecloud.plugin.command.shared.utilities.ApiUtils
import app.simplecloud.plugin.command.shared.utilities.tags
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.incendo.cloud.CommandManager
import org.incendo.cloud.kotlin.coroutines.extension.suspendingHandler
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.permission.Permission

class GroupCommand<C : CloudSender>(
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
                .literal("group")
                .literal("list")
                .suspendingHandler(plugin.scope) { context ->
                    val sender = context.sender()
                    val messages = plugin.messageConfiguration
                    val groups = api.group().allGroups.await()
                    if (groups.isEmpty()) {
                        sender.sendMessage(messages.msg(messages.command.group.list.empty))
                        return@suspendingHandler
                    }
                    val serversByGroup = api.server().allServers.await().groupBy { it.serverGroupId }
                    sender.sendMessage(messages.msg(messages.command.group.list.title, tags("count" to groups.size)))
                    groups.forEach { group ->
                        val servers = serversByGroup[group.serverGroupId].orEmpty()
                        val online = servers.count { it.state == ServerState.AVAILABLE || it.state == ServerState.INGAME }
                        sender.sendMessage(messages.msg(
                            messages.command.group.list.entry,
                            tags("group" to group.name, "online" to online, "total" to servers.size)
                        ))
                    }
                }
                .permission(Permission.permission(CommandPermissions.GROUP_LIST))
                .build()
        )
    }

    private fun registerInfo() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("group")
                .literal("info")
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
                    val servers = api.server().getServersByGroup(group.name).await()
                    sender.sendMessage(messages.msg(messages.command.group.info.title, tags("group" to group.name)))
                    sender.sendMessage(messages.msg(messages.command.group.info.entry, tags("key" to "Type", "value" to group.type.name)))
                    sender.sendMessage(messages.msg(messages.command.group.info.entry, tags("key" to "Memory", "value" to "${group.minMemory}-${group.maxMemory} MB")))
                    sender.sendMessage(messages.msg(messages.command.group.info.entry, tags("key" to "Max Players", "value" to group.maxPlayers)))
                    sender.sendMessage(messages.msg(messages.command.group.info.entry, tags("key" to "Running Servers", "value" to servers.size)))
                }
                .permission(Permission.permission(CommandPermissions.GROUP_INFO))
                .build()
        )
    }

    private fun registerStart() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("group")
                .literal("start")
                .required("group", stringParser(), CloudSuggestions.groups(api))
                .optional("count", integerParser(1, MAX_START_COUNT))
                .suspendingHandler(plugin.scope) { context ->
                    val sender = context.sender()
                    val groupName = context.get<String>("group")
                    val count = context.getOrDefault("count", 1)
                    val messages = plugin.messageConfiguration
                    val group = api.group().allGroups.await().firstOrNull { it.name.equals(groupName, true) }
                    if (group == null) {
                        sender.sendMessage(messages.msg(messages.command.group.error.notFound, tags("group" to groupName)))
                        return@suspendingHandler
                    }
                    val results = coroutineScope {
                        List(count) { async { runCatching { api.group().requestServerStart(group).await() } } }.awaitAll()
                    }
                    val failure = results.firstNotNullOfOrNull { it.exceptionOrNull() }
                    val started = results.count { it.isSuccess }
                    when {
                        failure == null -> sender.sendMessage(messages.msg(messages.command.group.start.success, tags("count" to count, "group" to group.name)))
                        started == 0 -> sender.sendMessage(messages.msg(
                            messages.command.group.error.startFailed,
                            tags("group" to group.name, "reason" to ApiUtils.reason(failure))
                        ))
                        else -> sender.sendMessage(messages.msg(
                            messages.command.group.start.partial,
                            tags("count" to started, "total" to count, "group" to group.name, "reason" to ApiUtils.reason(failure))
                        ))
                    }
                }
                .permission(Permission.permission(CommandPermissions.GROUP_START))
                .build()
        )
    }

    private fun registerStop() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("group")
                .literal("stop")
                .required("group", stringParser(), CloudSuggestions.groups(api))
                .optional("id", integerParser(1), CloudSuggestions.serverIds(api))
                .suspendingHandler(plugin.scope) { context ->
                    val sender = context.sender()
                    val groupName = context.get<String>("group")
                    val id = context.getOrDefault("id", null as Int?)
                    val messages = plugin.messageConfiguration
                    val group = api.group().allGroups.await().firstOrNull { it.name.equals(groupName, true) }
                    if (group == null) {
                        sender.sendMessage(messages.msg(messages.command.group.error.notFound, tags("group" to groupName)))
                        return@suspendingHandler
                    }
                    val servers = api.server().getServersByGroup(group.name).await()
                    val matching = if (id == null) servers else servers.filter { it.numericalId == id }
                    if (id != null && matching.isEmpty()) {
                        sender.sendMessage(messages.msg(messages.command.server.error.notFound, tags("group" to group.name, "id" to id)))
                        return@suspendingHandler
                    }
                    val targets = matching.filter { it.state != ServerState.STOPPING && it.state != ServerState.CLEANUP }
                    if (targets.isEmpty()) {
                        if (id == null) {
                            sender.sendMessage(messages.msg(messages.command.group.error.alreadyEmpty, tags("group" to group.name)))
                        } else {
                            sender.sendMessage(messages.msg(messages.command.server.error.alreadyStopping, tags("group" to group.name, "id" to id)))
                        }
                        return@suspendingHandler
                    }
                    val results = coroutineScope {
                        targets.map { server -> async { runCatching { api.server().stopServer(server.serverId).await() } } }.awaitAll()
                    }
                    val failure = results.firstNotNullOfOrNull { it.exceptionOrNull() }
                    val stopped = results.count { it.isSuccess }
                    when {
                        failure == null && id != null -> sender.sendMessage(messages.msg(messages.command.group.stop.successWithIds, tags("group" to group.name, "ids" to id)))
                        failure == null -> sender.sendMessage(messages.msg(messages.command.group.stop.success, tags("group" to group.name)))
                        stopped == 0 -> sender.sendMessage(messages.msg(
                            messages.command.group.error.stopFailed,
                            tags("group" to group.name, "reason" to ApiUtils.reason(failure))
                        ))
                        else -> sender.sendMessage(messages.msg(
                            messages.command.group.stop.partial,
                            tags("count" to stopped, "total" to targets.size, "group" to group.name, "reason" to ApiUtils.reason(failure))
                        ))
                    }
                }
                .permission(Permission.permission(CommandPermissions.GROUP_STOP))
                .build()
        )
    }

    companion object {
        private const val MAX_START_COUNT = 50
    }
}
