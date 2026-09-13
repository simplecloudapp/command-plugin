package app.simplecloud.plugin.command.shared.command.commands

import app.simplecloud.api.CloudApi
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
                .suspendingHandler { context ->
                    val sender = context.sender()
                    val messages = plugin.messageConfiguration
                    try {
                        val groups = api.group().allGroups.await()
                        if (groups.isEmpty()) {
                            sender.sendMessage(messages.msg(messages.command.group.list.empty))
                            return@suspendingHandler
                        }
                        sender.sendMessage(messages.msg(messages.command.group.list.title, tags("count" to groups.size)))
                        groups.forEach { group ->
                            val serverCount = api.server().getServersByGroup(group.name).await().size
                            sender.sendMessage(messages.msg(
                                messages.command.group.list.entry,
                                tags("group" to group.name, "value" to "$serverCount online")
                            ))
                        }
                    } catch (_: Exception) {
                        sender.sendMessage(messages.msg(messages.command.error.cloudUnavailable))
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
                        val servers = api.server().getServersByGroup(group.name).await()
                        sender.sendMessage(messages.msg(messages.command.group.info.title, tags("group" to group.name)))
                        sender.sendMessage(messages.msg(messages.command.group.info.entry, tags("key" to "Type", "value" to group.type.name)))
                        sender.sendMessage(messages.msg(messages.command.group.info.entry, tags("key" to "Memory", "value" to "${group.minMemory}-${group.maxMemory} MB")))
                        sender.sendMessage(messages.msg(messages.command.group.info.entry, tags("key" to "Max Players", "value" to group.maxPlayers)))
                        sender.sendMessage(messages.msg(messages.command.group.info.entry, tags("key" to "Running Servers", "value" to servers.size)))
                    } catch (_: Exception) {
                        sender.sendMessage(messages.msg(messages.command.error.internal))
                    }
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
                .optional("count", integerParser())
                .suspendingHandler { context ->
                    val sender = context.sender()
                    val groupName = context.get<String>("group")
                    val count = context.getOrDefault("count", 1)
                    val messages = plugin.messageConfiguration
                    if (count <= 0) {
                        sender.sendMessage(messages.msg(messages.command.usage.invalidNumber, tags("value" to count)))
                        return@suspendingHandler
                    }
                    try {
                        val group = api.group().getGroupByName(groupName).await()
                        if (group == null) {
                            sender.sendMessage(messages.msg(messages.command.group.error.notFound, tags("group" to groupName)))
                            return@suspendingHandler
                        }
                        try {
                            repeat(count) {
                                api.group().requestServerStart(group.serverGroupId).await()
                            }
                            sender.sendMessage(messages.msg(messages.command.group.start.success, tags("count" to count, "group" to group.name)))
                        } catch (_: Exception) {
                            repeat(count) {
                                api.group().requestServerStart(group).await()
                            }
                            sender.sendMessage(messages.msg(messages.command.group.start.queued, tags("group" to group.name)))
                        }
                    } catch (_: Exception) {
                        sender.sendMessage(messages.msg(messages.command.group.error.startFailed, tags("group" to groupName)))
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
                .optional("id", integerParser(), CloudSuggestions.serverIds(api))
                .suspendingHandler { context ->
                    val sender = context.sender()
                    val groupName = context.get<String>("group")
                    val id = context.getOrDefault("id", null as Int?)
                    val messages = plugin.messageConfiguration
                    try {
                        val group = api.group().getGroupByName(groupName).await()
                        if (group == null) {
                            sender.sendMessage(messages.msg(messages.command.group.error.notFound, tags("group" to groupName)))
                            return@suspendingHandler
                        }
                        val servers = api.server().getServersByGroup(group.name).await()
                        if (servers.isEmpty()) {
                            sender.sendMessage(messages.msg(messages.command.group.error.alreadyEmpty, tags("group" to group.name)))
                            return@suspendingHandler
                        }
                        if (id != null) {
                            val server = servers.find { it.numericalId == id }
                            if (server == null) {
                                sender.sendMessage(messages.msg(messages.command.server.error.notFound, tags("group" to group.name, "id" to id)))
                                return@suspendingHandler
                            }
                            api.server().stopServer(server.serverId).await()
                            sender.sendMessage(messages.msg(messages.command.group.stop.successWithIds, tags("group" to group.name, "ids" to id)))
                        } else {
                            servers.forEach { api.server().stopServer(it.serverId).await() }
                            sender.sendMessage(messages.msg(messages.command.group.stop.success, tags("group" to group.name)))
                        }
                    } catch (_: Exception) {
                        sender.sendMessage(messages.msg(messages.command.group.error.stopFailed, tags("group" to groupName)))
                    }
                }
                .permission(Permission.permission(CommandPermissions.GROUP_STOP))
                .build()
        )
    }
}