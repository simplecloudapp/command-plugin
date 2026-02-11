package app.simplecloud.plugin.command.shared.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.command.shared.CloudSender
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.shared.miniMessage
import app.simplecloud.plugin.command.shared.resolver.GroupTagResolver
import app.simplecloud.plugin.command.shared.resolver.PersistentServerTagResolver
import app.simplecloud.plugin.command.shared.resolver.ServerTagResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.permission.Permission
import org.incendo.cloud.suggestion.Suggestion
import java.util.concurrent.CompletableFuture

class InfoCommand(
    private val api: CloudApi,
    private val plugin: CommandPlugin
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun <C : CloudSender> register(commandManager: CommandManager<C>) {
        registerServerInfo(commandManager)
        registerGroupInfo(commandManager)
        registerPersistentServerInfo(commandManager)
    }

    private fun <C : CloudSender> registerServerInfo(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("info")
                .literal("servers", "server")
                .optional("group", stringParser()) { _, _ ->
                    api.group().allGroups.thenApply { groups ->
                        groups.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .optional("id", integerParser()) { context, _ ->
                    try {
                        val group = context.get<String>("group")
                        api.server().getServersByGroup(group).thenApply { servers ->
                            servers.map { Suggestion.suggestion(it.numericalId.toString()) }
                        }.exceptionally { emptyList() }
                    } catch (e: Exception) {
                        CompletableFuture.completedFuture(emptyList())
                    }
                }
                .handler { context ->
                    val groupName = context.getOrDefault("group", null as String?)
                    val id = context.getOrDefault("id", null as Int?)
                    scope.launch {
                        try {
                            when {
                                groupName != null && id != null -> showServerDetail(context.sender(), groupName, id)
                                groupName != null -> showGroupServers(context.sender(), groupName)
                                else -> showAllServers(context.sender())
                            }
                        } catch (e: Exception) {
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.errorMessage, Placeholder.unparsed("error", e.message ?: "Unknown error")))
                        }
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.info.servers"))
                .build()
        )
    }

    private suspend fun showServerDetail(sender: CloudSender, groupName: String, id: Int) {
        val server = api.server().getServerByNumericalId(groupName, id).await()
        val resolver = ServerTagResolver.of(server)
        sender.sendMessage(miniMessage(plugin.messageConfiguration.serverInfoTitle, resolver))
        sender.sendMessage(miniMessage(plugin.messageConfiguration.serverInfoState, resolver))
        sender.sendMessage(miniMessage(plugin.messageConfiguration.serverInfoMemory, resolver))
        sender.sendMessage(miniMessage(plugin.messageConfiguration.serverInfoPlayers, resolver))
    }

    private suspend fun showGroupServers(sender: CloudSender, groupName: String) {
        val group = api.group().getGroupByName(groupName).await()
        val servers = api.server().getServersByGroup(groupName).await()
        sender.sendMessage(miniMessage(plugin.messageConfiguration.groupServerListTitle, GroupTagResolver.of(group)))
        servers.forEach { server ->
            sender.sendMessage(miniMessage(plugin.messageConfiguration.groupServerListEntry, ServerTagResolver.of(server)))
        }
    }

    private suspend fun showAllServers(sender: CloudSender) {
        val servers = api.server().allServers.await()
        sender.sendMessage(miniMessage(plugin.messageConfiguration.serverListTitle))
        servers.forEach { server ->
            sender.sendMessage(miniMessage(plugin.messageConfiguration.serverListEntry, ServerTagResolver.of(server)))
        }
    }

    private fun <C : CloudSender> registerGroupInfo(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("info")
                .literal("groups", "group")
                .optional("group", stringParser()) { _, _ ->
                    api.group().allGroups.thenApply { groups ->
                        groups.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .handler { context ->
                    val groupName = context.getOrDefault("group", null as String?)
                    scope.launch {
                        try {
                            if (groupName != null) {
                                showGroupDetail(context.sender(), groupName)
                            } else {
                                showAllGroups(context.sender())
                            }
                        } catch (e: Exception) {
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.errorMessage, Placeholder.unparsed("error", e.message ?: "Unknown error")))
                        }
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.info.groups"))
                .build()
        )
    }

    private suspend fun showGroupDetail(sender: CloudSender, groupName: String) {
        val group = api.group().getGroupByName(groupName).await()
        val servers = api.server().getServersByGroup(groupName).await()
        val resolver = GroupTagResolver.of(group)
        val countPlaceholder = Placeholder.unparsed("server_count", servers.size.toString())
        sender.sendMessage(miniMessage(plugin.messageConfiguration.groupInfoTitle, resolver, countPlaceholder))
        sender.sendMessage(miniMessage(plugin.messageConfiguration.groupInfoType, resolver))
        sender.sendMessage(miniMessage(plugin.messageConfiguration.groupInfoMemory, resolver))
        sender.sendMessage(miniMessage(plugin.messageConfiguration.groupInfoPlayers, resolver))
    }

    private suspend fun showAllGroups(sender: CloudSender) {
        val groups = api.group().allGroups.await()
        sender.sendMessage(miniMessage(plugin.messageConfiguration.groupsListTitle))
        groups.forEach { group ->
            val servers = api.server().getServersByGroup(group.name).await()
            sender.sendMessage(miniMessage(plugin.messageConfiguration.groupsListEntry, GroupTagResolver.of(group), Placeholder.unparsed("server_count", servers.size.toString())))
        }
    }

    private fun <C : CloudSender> registerPersistentServerInfo(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("info")
                .literal("persistent")
                .optional("server", stringParser()) { _, _ ->
                    api.persistentServer().allPersistentServers.thenApply { servers ->
                        servers.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .handler { context ->
                    val serverName = context.getOrDefault("server", null as String?)
                    scope.launch {
                        try {
                            if (serverName != null) {
                                showPersistentServerDetail(context.sender(), serverName)
                            } else {
                                showAllPersistentServers(context.sender())
                            }
                        } catch (e: Exception) {
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.errorMessage, Placeholder.unparsed("error", e.message ?: "Unknown error")))
                        }
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.info.persistent"))
                .build()
        )
    }

    private suspend fun showPersistentServerDetail(sender: CloudSender, serverName: String) {
        val persistent = api.persistentServer().getPersistentServerByName(serverName).await()
        val resolver = PersistentServerTagResolver.of(persistent)
        sender.sendMessage(miniMessage(plugin.messageConfiguration.persistentServerInfoTitle, resolver))
        sender.sendMessage(miniMessage(plugin.messageConfiguration.persistentServerInfoType, resolver))
        sender.sendMessage(miniMessage(plugin.messageConfiguration.persistentServerInfoMemory, resolver))
        sender.sendMessage(miniMessage(plugin.messageConfiguration.persistentServerInfoPlayers, resolver))
        sender.sendMessage(miniMessage(plugin.messageConfiguration.persistentServerInfoActive, resolver))
        sender.sendMessage(miniMessage(plugin.messageConfiguration.persistentServerInfoServerhost, resolver))
    }

    private suspend fun showAllPersistentServers(sender: CloudSender) {
        val servers = api.persistentServer().allPersistentServers.await()
        sender.sendMessage(miniMessage(plugin.messageConfiguration.persistentServersListTitle))
        servers.forEach { server ->
            sender.sendMessage(miniMessage(plugin.messageConfiguration.persistentServersListEntry, PersistentServerTagResolver.of(server)))
        }
    }
}
