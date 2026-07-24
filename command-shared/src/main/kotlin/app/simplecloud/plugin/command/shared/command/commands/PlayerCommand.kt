package app.simplecloud.plugin.command.shared.command.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.api.player.CloudPlayer
import app.simplecloud.plugin.command.shared.command.CloudSender
import app.simplecloud.plugin.command.shared.utilities.CommandPermissions
import app.simplecloud.plugin.command.shared.CommandPlugin
import app.simplecloud.plugin.command.shared.utilities.tags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.incendo.cloud.CommandManager
import org.incendo.cloud.parser.standard.StringParser.StringMode
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.permission.Permission
import org.incendo.cloud.suggestion.Suggestion
import java.util.concurrent.CompletableFuture

class PlayerCommand(
    private val api: CloudApi,
    private val plugin: CommandPlugin
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun <C : CloudSender> register(commandManager: CommandManager<C>) {
        registerList(commandManager)
        registerInfo(commandManager)
        registerSend(commandManager)
        registerSendAll(commandManager)
        registerSendFrom(commandManager)
        registerMessage(commandManager)
    }

    private fun <C : CloudSender> registerList(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("player")
                .literal("list")
                .required("targetType", stringParser()) { _, _ ->
                    CompletableFuture.completedFuture(listOf(Suggestion.suggestion("group"), Suggestion.suggestion("ps")))
                }
                .required("target", stringParser())
                .handler { context ->
                    val targetType = context.get<String>("targetType")
                    val target = context.get<String>("target")
                    val messages = plugin.messageConfiguration
                    scope.launch {
                        try {
                            if (!isTargetTypeValid(targetType)) {
                                context.sender().sendMessage(messages.msg(messages.command.usage.invalidTargetType))
                                return@launch
                            }
                            if (!targetExists(targetType, target)) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.targetNotFound, tags("target" to target))
                                )
                                return@launch
                            }
                            val players = playersForTarget(targetType, target)
                            if (players.isEmpty()) {
                                context.sender().sendMessage(messages.msg(messages.command.player.list.empty))
                                return@launch
                            }
                            context.sender().sendMessage(
                                messages.msg(messages.command.player.list.title, tags("count" to players.size))
                            )
                            players.forEach { player ->
                                context.sender().sendMessage(
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
                        } catch (e: Exception) {
                            e.printStackTrace()
                            context.sender().sendMessage(messages.msg(messages.command.error.cloudUnavailable))
                        }
                    }
                }
                .permission(Permission.permission(CommandPermissions.PLAYER_LIST))
                .build()
        )
    }

    private fun <C : CloudSender> registerInfo(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("player")
                .literal("info")
                .required("player", stringParser()) { _, _ ->
                    api.player().onlinePlayers.thenApply { players ->
                        players.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .handler { context ->
                    val playerName = context.get<String>("player")
                    val messages = plugin.messageConfiguration
                    scope.launch {
                        try {
                            val player = api.player().get(playerName).await()
                            if (player == null) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.notFound, tags("playername" to playerName))
                                )
                                return@launch
                            }
                            context.sender().sendMessage(
                                messages.msg(
                                    messages.command.player.info.title,
                                    tags("playername" to player.name, "displayname" to player.displayName)
                                )
                            )
                            context.sender().sendMessage(
                                messages.msg(messages.command.player.info.entry, tags("key" to "Name", "value" to player.name))
                            )
                            context.sender().sendMessage(
                                messages.msg(messages.command.player.info.entry, tags("key" to "Display Name", "value" to player.displayName))
                            )
                            context.sender().sendMessage(
                                messages.msg(messages.command.player.info.entry, tags("key" to "Online", "value" to player.isOnline))
                            )
                            context.sender().sendMessage(
                                messages.msg(messages.command.player.info.entry, tags("key" to "Server", "value" to player.connectedServerName))
                            )
                            context.sender().sendMessage(
                                messages.msg(messages.command.player.info.entry, tags("key" to "Proxy", "value" to player.connectedProxyName))
                            )
                        } catch (_: Exception) {
                            context.sender().sendMessage(messages.msg(messages.command.error.internal))
                        }
                    }
                }
                .permission(Permission.permission(CommandPermissions.PLAYER_INFO))
                .build()
        )
    }

    private fun <C : CloudSender> registerSend(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("player")
                .literal("send")
                .required("player", stringParser()) { _, _ ->
                    api.player().onlinePlayers.thenApply { players ->
                        players.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .required("targetType", stringParser()) { _, _ ->
                    CompletableFuture.completedFuture(listOf(Suggestion.suggestion("group"), Suggestion.suggestion("ps")))
                }
                .required("target", stringParser())
                .handler { context ->
                    val playerName = context.get<String>("player")
                    val targetType = context.get<String>("targetType")
                    val target = context.get<String>("target")
                    val messages = plugin.messageConfiguration
                    scope.launch {
                        try {
                            if (!isTargetTypeValid(targetType)) {
                                context.sender().sendMessage(messages.msg(messages.command.usage.invalidTargetType))
                                return@launch
                            }
                            val player = api.player().get(playerName).await()
                            if (player == null) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.notFound, tags("playername" to playerName))
                                )
                                return@launch
                            }
                            if (!player.isOnline) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.notOnline, tags("playername" to player.name))
                                )
                                return@launch
                            }
                            if (!targetExists(targetType, target)) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.targetNotFound, tags("target" to target))
                                )
                                return@launch
                            }
                            val result = player.connect(target).await()
                            when (result.name) {
                                "SUCCESS" -> context.sender().sendMessage(
                                    messages.msg(
                                        messages.command.player.send.success,
                                        tags("playername" to player.name, "displayname" to player.displayName, "target" to target)
                                    )
                                )

                                "ALREADY_CONNECTED" -> context.sender().sendMessage(
                                    messages.msg(
                                        messages.command.player.error.sameTarget,
                                        tags("playername" to player.name, "displayname" to player.displayName, "target" to target)
                                    )
                                )

                                "SERVER_NOT_FOUND" -> context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.targetNotFound, tags("target" to target))
                                )

                                "PLAYER_NOT_FOUND" -> context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.notOnline, tags("playername" to player.name))
                                )

                                else -> context.sender().sendMessage(
                                    messages.msg(
                                        messages.command.player.error.sendFailed,
                                        tags("playername" to player.name, "displayname" to player.displayName, "target" to target)
                                    )
                                )
                            }
                        } catch (_: Exception) {
                            context.sender().sendMessage(messages.msg(messages.command.error.internal))
                        }
                    }
                }
                .permission(Permission.permission(CommandPermissions.PLAYER_SEND))
                .build()
        )
    }

    private fun <C : CloudSender> registerSendAll(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("player")
                .literal("sendall")
                .required("targetType", stringParser()) { _, _ ->
                    CompletableFuture.completedFuture(listOf(Suggestion.suggestion("group"), Suggestion.suggestion("ps")))
                }
                .required("target", stringParser())
                .handler { context ->
                    val targetType = context.get<String>("targetType")
                    val target = context.get<String>("target")
                    val messages = plugin.messageConfiguration
                    scope.launch {
                        try {
                            if (!isTargetTypeValid(targetType)) {
                                context.sender().sendMessage(messages.msg(messages.command.usage.invalidTargetType))
                                return@launch
                            }
                            if (!targetExists(targetType, target)) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.targetNotFound, tags("target" to target))
                                )
                                return@launch
                            }
                            val players = api.player().onlinePlayers.await()
                            var success = 0
                            players.forEach { player ->
                                if (player.connect(target).await().name == "SUCCESS") {
                                    success++
                                }
                            }
                            if (success > 0) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.send.successAll, tags("count" to success, "target" to target))
                                )
                            } else {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.sendAllFailed, tags("target" to target))
                                )
                            }
                        } catch (_: Exception) {
                            context.sender().sendMessage(messages.msg(messages.command.error.internal))
                        }
                    }
                }
                .permission(Permission.permission(CommandPermissions.PLAYER_SEND))
                .build()
        )
    }

    private fun <C : CloudSender> registerSendFrom(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("player")
                .literal("sendfrom")
                .required("source", stringParser())
                .required("targetType", stringParser()) { _, _ ->
                    CompletableFuture.completedFuture(listOf(Suggestion.suggestion("group"), Suggestion.suggestion("ps")))
                }
                .required("target", stringParser())
                .handler { context ->
                    val source = context.get<String>("source")
                    val targetType = context.get<String>("targetType")
                    val target = context.get<String>("target")
                    val messages = plugin.messageConfiguration
                    scope.launch {
                        try {
                            if (!isTargetTypeValid(targetType)) {
                                context.sender().sendMessage(messages.msg(messages.command.usage.invalidTargetType))
                                return@launch
                            }
                            if (!targetExists(targetType, target)) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.targetNotFound, tags("target" to target))
                                )
                                return@launch
                            }
                            val normalServers = api.server().allServers.await()
                            val persistentServers = api.persistentServer().allPersistentServers.await()

                            val sourceServers = normalServers.filter {
                                matchesSource(it.serverId, it.group?.name, it.numericalId, source)
                            }

                            val sourcePersistentServers = persistentServers.filter {
                                it.persistentServerId.equals(source, true)
                            }

                            if (sourceServers.isEmpty() && sourcePersistentServers.isEmpty()) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.sourceNotFound, tags("source" to source))
                                )
                                return@launch
                            }
                            val sourceNames = sourceServers.map { "${it.group?.name ?: it.serverGroupId} ${it.numericalId}" }.toSet()
                            val players = api.player().onlinePlayers.await()
                                .filter { player ->
                                    sourceServers.any { it.serverId.equals(player.connectedServerName, true) } ||
                                        sourceNames.any { it.equals(player.connectedServerName, true) } ||
                                        sourcePersistentServers.any {
                                            it.persistentServerId.equals(player.connectedServerName, true)
                                        }
                                }
                            if (players.isEmpty()) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.noSourcePlayers, tags("source" to source))
                                )
                                return@launch
                            }
                            var success = 0
                            players.forEach { player ->
                                if (player.connect(target).await().name == "SUCCESS") {
                                    success++
                                }
                            }
                            if (success > 0) {
                                context.sender().sendMessage(
                                    messages.msg(
                                        messages.command.player.send.successFromServer,
                                        tags("count" to success, "source" to source, "target" to target)
                                    )
                                )
                            } else {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.sendAllFailed, tags("target" to target))
                                )
                            }
                        } catch (_: Exception) {
                            context.sender().sendMessage(messages.msg(messages.command.error.internal))
                        }
                    }
                }
                .permission(Permission.permission(CommandPermissions.PLAYER_SEND))
                .build()
        )
    }

    private fun <C : CloudSender> registerMessage(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud", "sc", "simplecloud")
                .literal("player")
                .literal("message")
                .required("player", stringParser()) { _, _ ->
                    api.player().onlinePlayers.thenApply { players ->
                        players.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .required("message", stringParser(StringMode.GREEDY))
                .handler { context ->
                    val playerName = context.get<String>("player")
                    val message = context.get<String>("message")
                    val messages = plugin.messageConfiguration
                    scope.launch {
                        try {
                            if (message.isBlank()) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.messageEmpty, tags("playername" to playerName))
                                )
                                return@launch
                            }
                            val player = api.player().get(playerName).await()
                            if (player == null || !player.isOnline) {
                                context.sender().sendMessage(
                                    messages.msg(messages.command.player.error.notOnline, tags("playername" to playerName))
                                )
                                return@launch
                            }
                            player.sendMessage(Component.text(message))
                            context.sender().sendMessage(
                                messages.msg(
                                    messages.command.player.message.success,
                                    tags("playername" to player.name, "displayname" to player.displayName)
                                )
                            )
                        } catch (_: Exception) {
                            context.sender().sendMessage(
                                messages.msg(messages.command.player.error.messageFailed, tags("playername" to playerName))
                            )
                        }
                    }
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
        val players = api.player().onlinePlayers.await()
        return when {
            targetType.equals("group", true) -> {
                val servers = api.server().getServersByGroup(target).await()
                val identifiers = servers.flatMap { server ->
                    listOf(
                        server.serverId,
                        "${server.group?.name ?: server.serverGroupId} ${server.numericalId}",
                        "${server.group?.name ?: server.serverGroupId}-${server.numericalId}"
                    )
                }.toSet()
                players.filter { player ->
                    identifiers.any { it.equals(player.connectedServerName, true) }
                }
            }

            targetType.equals("ps", true) -> {
                val servers = api.persistentServer().allPersistentServers.await()
                    .filter { it.persistentServerId == target }
                players.filter { player ->
                    servers.any { it.persistentServerId.equals(player.connectedServerName, true) }
                }
            }

            else -> emptyList()
        }
    }

    private fun matchesSource(serverId: String, groupName: String?, numericalId: Int, source: String): Boolean {
        val spaced = "${groupName ?: ""} $numericalId".trim()
        val dashed = "${groupName ?: ""}-$numericalId".trim()
        return serverId.equals(source, true) || spaced.equals(source, true) || dashed.equals(source, true)
    }
}
