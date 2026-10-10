package app.simplecloud.plugin.command.shared.command.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.api.persistentserver.PersistentServer
import app.simplecloud.api.player.CloudPlayer
import app.simplecloud.api.player.CloudPlayer.ConnectResult
import app.simplecloud.api.server.Server
import app.simplecloud.plugin.command.shared.command.CloudSender
import app.simplecloud.plugin.command.shared.utilities.CommandPermissions
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.shared.command.CloudSuggestions
import app.simplecloud.plugin.command.shared.utilities.tags
import kotlinx.coroutines.*
import kotlinx.coroutines.future.*
import net.kyori.adventure.text.Component
import org.incendo.cloud.CommandManager
import org.incendo.cloud.kotlin.coroutines.extension.suspendingHandler
import org.incendo.cloud.parser.standard.StringParser.StringMode
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.permission.Permission

class PlayerCommand<C : CloudSender>(
    private val api: CloudApi,
    private val plugin: CommandPlugin,
    private val manager: CommandManager<C>
) {

    fun register() {
        registerList()
        registerInfo()
        registerSend()
        registerSendAll()
        registerSendFrom()
        registerMessage()
    }

    private fun registerList() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("player")
                .literal("list")
                .required("targetType", stringParser(), CloudSuggestions.targetTypes())
                .required("target", stringParser())
                .suspendingHandler(plugin.scope) { context ->
                    val sender = context.sender()
                    val targetType = context.get<String>("targetType")
                    val target = context.get<String>("target")
                    val messages = plugin.messageConfiguration
                    if (!isTargetTypeValid(targetType)) {
                        sender.sendMessage(messages.msg(messages.command.usage.invalidTargetType))
                        return@suspendingHandler
                    }
                    if (!targetExists(targetType, target)) {
                        sender.sendMessage(messages.msg(messages.command.player.error.targetNotFound, tags("target" to target)))
                        return@suspendingHandler
                    }
                    val players = playersForTarget(targetType, target)
                    if (players.isEmpty()) {
                        sender.sendMessage(messages.msg(messages.command.player.list.empty))
                        return@suspendingHandler
                    }
                    sender.sendMessage(messages.msg(messages.command.player.list.title, tags("count" to players.size)))
                    players.forEach { player ->
                        sender.sendMessage(
                            messages.msg(
                                messages.command.player.list.entry,
                                tags(
                                    "playername" to player.name,
                                    "displayname" to player.displayName,
                                    "value" to player.connectedServerName
                                )
                            )
                        )
                    }
                }
                .permission(Permission.permission(CommandPermissions.PLAYER_LIST))
                .build()
        )
    }

    private fun registerInfo() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("player")
                .literal("info")
                .required("player", stringParser(), CloudSuggestions.onlinePlayers(api))
                .suspendingHandler(plugin.scope) { context ->
                    val sender = context.sender()
                    val playerName = context.get<String>("player")
                    val messages = plugin.messageConfiguration
                    val player = api.player().get(playerName).await()
                    if (player == null) {
                        sender.sendMessage(messages.msg(messages.command.player.error.notFound, tags("playername" to playerName)))
                        return@suspendingHandler
                    }
                    sender.sendMessage(messages.msg(messages.command.player.info.title, tags("playername" to player.name, "displayname" to player.displayName)))
                    sender.sendMessage(messages.msg(messages.command.player.info.entry, tags("key" to "Name", "value" to player.name)))
                    sender.sendMessage(messages.msg(messages.command.player.info.entry, tags("key" to "Display Name", "value" to player.displayName)))
                    sender.sendMessage(messages.msg(messages.command.player.info.entry, tags("key" to "Online", "value" to player.isOnline)))
                    sender.sendMessage(messages.msg(messages.command.player.info.entry, tags("key" to "Server", "value" to player.connectedServerName)))
                    sender.sendMessage(messages.msg(messages.command.player.info.entry, tags("key" to "Proxy", "value" to player.connectedProxyName)))
                }
                .permission(Permission.permission(CommandPermissions.PLAYER_INFO))
                .build()
        )
    }

    private fun registerSend() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("player")
                .literal("send")
                .required("player", stringParser(), CloudSuggestions.onlinePlayers(api))
                .required("targetType", stringParser(), CloudSuggestions.targetTypes())
                .required("target", stringParser())
                .suspendingHandler(plugin.scope) { context ->
                    val sender = context.sender()
                    val playerName = context.get<String>("player")
                    val targetType = context.get<String>("targetType")
                    val target = context.get<String>("target")
                    val messages = plugin.messageConfiguration
                    if (!isTargetTypeValid(targetType)) {
                        sender.sendMessage(messages.msg(messages.command.usage.invalidTargetType))
                        return@suspendingHandler
                    }
                    val player = api.player().get(playerName).await()
                    if (player == null) {
                        sender.sendMessage(messages.msg(messages.command.player.error.notFound, tags("playername" to playerName)))
                        return@suspendingHandler
                    }
                    if (!player.isOnline) {
                        sender.sendMessage(messages.msg(messages.command.player.error.notOnline, tags("playername" to player.name)))
                        return@suspendingHandler
                    }
                    if (!targetExists(targetType, target)) {
                        sender.sendMessage(messages.msg(messages.command.player.error.targetNotFound, tags("target" to target)))
                        return@suspendingHandler
                    }
                    val result = player.connect(target).await()
                    when (result) {
                        ConnectResult.SUCCESS -> sender.sendMessage(
                            messages.msg(
                                messages.command.player.send.success,
                                tags("playername" to player.name, "displayname" to player.displayName, "target" to target)
                            )
                        )

                        ConnectResult.ALREADY_CONNECTED -> sender.sendMessage(
                            messages.msg(
                                messages.command.player.error.sameTarget,
                                tags("playername" to player.name, "displayname" to player.displayName, "target" to target)
                            )
                        )

                        ConnectResult.SERVER_NOT_FOUND -> sender.sendMessage(
                            messages.msg(messages.command.player.error.targetNotFound, tags("target" to target))
                        )

                        ConnectResult.PLAYER_NOT_FOUND -> sender.sendMessage(
                            messages.msg(messages.command.player.error.notOnline, tags("playername" to player.name))
                        )

                        else -> sender.sendMessage(
                            messages.msg(
                                messages.command.player.error.sendFailed,
                                tags("playername" to player.name, "displayname" to player.displayName, "target" to target)
                            )
                        )
                    }
                }
                .permission(Permission.permission(CommandPermissions.PLAYER_SEND))
                .build()
        )
    }

    private fun registerSendAll() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("player")
                .literal("sendall")
                .required("targetType", stringParser(), CloudSuggestions.targetTypes())
                .required("target", stringParser())
                .suspendingHandler(plugin.scope) { context ->
                    val sender = context.sender()
                    val targetType = context.get<String>("targetType")
                    val target = context.get<String>("target")
                    val messages = plugin.messageConfiguration
                    if (!isTargetTypeValid(targetType)) {
                        sender.sendMessage(messages.msg(messages.command.usage.invalidTargetType))
                        return@suspendingHandler
                    }
                    if (!targetExists(targetType, target)) {
                        sender.sendMessage(messages.msg(messages.command.player.error.targetNotFound, tags("target" to target)))
                        return@suspendingHandler
                    }
                    val results = connectAll(api.player().onlinePlayers.await(), target)
                    val sent = results.count { it == ConnectResult.SUCCESS }
                    when {
                        sent > 0 -> sender.sendMessage(messages.msg(messages.command.player.send.successAll, tags("count" to sent, "target" to target)))
                        results.isNotEmpty() && results.all { it == ConnectResult.ALREADY_CONNECTED } ->
                            sender.sendMessage(messages.msg(messages.command.player.error.allOnTarget, tags("target" to target)))
                        else -> sender.sendMessage(messages.msg(messages.command.player.error.sendAllFailed, tags("target" to target)))
                    }
                }
                .permission(Permission.permission(CommandPermissions.PLAYER_SEND))
                .build()
        )
    }

    private fun registerSendFrom() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("player")
                .literal("sendfrom")
                .required("source", stringParser())
                .required("targetType", stringParser(), CloudSuggestions.targetTypes())
                .required("target", stringParser())
                .suspendingHandler(plugin.scope) { context ->
                    val sender = context.sender()
                    val source = context.get<String>("source")
                    val targetType = context.get<String>("targetType")
                    val target = context.get<String>("target")
                    val messages = plugin.messageConfiguration
                    if (!isTargetTypeValid(targetType)) {
                        sender.sendMessage(messages.msg(messages.command.usage.invalidTargetType))
                        return@suspendingHandler
                    }
                    if (!targetExists(targetType, target)) {
                        sender.sendMessage(messages.msg(messages.command.player.error.targetNotFound, tags("target" to target)))
                        return@suspendingHandler
                    }
                    val sourceKey = source.lowercase()
                    val sourceIdentifiers = api.server().allServers.await()
                        .asSequence()
                        .map { serverIdentifiers(it) }
                        .plus(api.persistentServer().allPersistentServers.await().map { serverIdentifiers(it) })
                        .filter { sourceKey in it }
                        .flatten()
                        .toSet()
                    if (sourceIdentifiers.isEmpty()) {
                        sender.sendMessage(messages.msg(messages.command.player.error.sourceNotFound, tags("source" to source)))
                        return@suspendingHandler
                    }
                    val players = playersOn(sourceIdentifiers)
                    if (players.isEmpty()) {
                        sender.sendMessage(messages.msg(messages.command.player.error.noSourcePlayers, tags("source" to source)))
                        return@suspendingHandler
                    }
                    val results = connectAll(players, target)
                    val sent = results.count { it == ConnectResult.SUCCESS }
                    when {
                        sent > 0 -> sender.sendMessage(messages.msg(messages.command.player.send.successFromServer, tags("count" to sent, "source" to source, "target" to target)))
                        results.all { it == ConnectResult.ALREADY_CONNECTED } ->
                            sender.sendMessage(messages.msg(messages.command.player.error.allOnTarget, tags("target" to target)))
                        else -> sender.sendMessage(messages.msg(messages.command.player.error.sendAllFailed, tags("target" to target)))
                    }
                }
                .permission(Permission.permission(CommandPermissions.PLAYER_SEND))
                .build()
        )
    }

    private fun registerMessage() {
        manager.command(
            manager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("player")
                .literal("message")
                .required("player", stringParser(), CloudSuggestions.onlinePlayers(api))
                .required("message", stringParser(StringMode.GREEDY))
                .suspendingHandler(plugin.scope) { context ->
                    val sender = context.sender()
                    val playerName = context.get<String>("player")
                    val message = context.get<String>("message")
                    val messages = plugin.messageConfiguration
                    if (message.isBlank()) {
                        sender.sendMessage(messages.msg(messages.command.player.error.messageEmpty, tags("playername" to playerName)))
                        return@suspendingHandler
                    }
                    val player = api.player().get(playerName).await()
                    if (player == null || !player.isOnline) {
                        sender.sendMessage(messages.msg(messages.command.player.error.notOnline, tags("playername" to playerName)))
                        return@suspendingHandler
                    }
                    runCatching { player.sendMessage(Component.text(message)) }
                        .onSuccess { sender.sendMessage(messages.msg(messages.command.player.message.success, tags("playername" to player.name, "displayname" to player.displayName))) }
                        .onFailure { sender.sendMessage(messages.msg(messages.command.player.error.messageFailed, tags("playername" to player.name))) }
                }
                .permission(Permission.permission(CommandPermissions.PLAYER_MESSAGE))
                .build()
        )
    }

    private fun isTargetTypeValid(targetType: String): Boolean {
        return targetType.equals("group", true) || targetType.equals("ps", true)
    }

    private suspend fun targetExists(targetType: String, target: String): Boolean {
        return when {
            targetType.equals("group", true) -> api.group().getGroupByName(target).await() != null
            targetType.equals("ps", true) -> api.persistentServer().getPersistentServerByName(target).await() != null
            else -> false
        }
    }

    private suspend fun playersForTarget(targetType: String, target: String): List<CloudPlayer> {
        if (targetType.equals("group", true)) {
            return playersOn(api.server().getServersByGroup(target).await().flatMap { serverIdentifiers(it) }.toSet())
        }
        val persistentServer = api.persistentServer().getPersistentServerByName(target).await() ?: return emptyList()
        return playersOn(serverIdentifiers(persistentServer))
    }

    private suspend fun playersOn(identifiers: Set<String>): List<CloudPlayer> =
        api.player().onlinePlayers.await().filter { it.connectedServerName?.lowercase() in identifiers }

    private suspend fun connectAll(players: List<CloudPlayer>, target: String): List<ConnectResult?> = coroutineScope {
        players.map { player -> async { runCatching { player.connect(target).await() }.getOrNull() } }.awaitAll()
    }

    private fun serverIdentifiers(server: Server): Set<String> {
        val groupName = server.group?.name ?: server.serverGroupId
        val numbered = if (groupName.isNullOrBlank()) {
            emptyList()
        } else {
            listOf("$groupName ${server.numericalId}", "$groupName-${server.numericalId}")
        }
        return (listOfNotNull(server.serverId, server.persistentServerId, server.persistentServer?.name) + numbered)
            .filter { it.isNotBlank() }
            .map { it.lowercase() }
            .toSet()
    }

    private fun serverIdentifiers(server: PersistentServer): Set<String> =
        listOfNotNull(server.persistentServerId, server.name)
            .filter { it.isNotBlank() }
            .map { it.lowercase() }
            .toSet()
}
