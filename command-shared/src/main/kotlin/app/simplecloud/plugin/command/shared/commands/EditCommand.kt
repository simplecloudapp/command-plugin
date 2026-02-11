package app.simplecloud.plugin.command.shared.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.api.group.DeploymentConfig
import app.simplecloud.api.group.DeploymentHost
import app.simplecloud.api.group.SourceConfig
import app.simplecloud.api.group.WorkflowsConfig
import app.simplecloud.api.group.WorkflowWhen
import app.simplecloud.api.group.UpdateGroupRequest
import app.simplecloud.api.group.ScalingConfig
import app.simplecloud.api.group.ScaleDownConfig
import app.simplecloud.api.persistentserver.PersistentServer
import app.simplecloud.api.persistentserver.UpdatePersistentServerRequest
import app.simplecloud.api.server.UpdateServerRequest
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

class EditCommand(
    private val api: CloudApi,
    private val plugin: CommandPlugin
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val groupSettings = listOf("max-players", "min-memory", "max-memory")
    private val serverSettings = listOf("max-players", "min-memory", "max-memory")
    private val persistentSettings = listOf("max-players", "min-memory", "max-memory", "active")

    fun <C : CloudSender> register(commandManager: CommandManager<C>) {
        registerEditGroup(commandManager)
        registerEditServer(commandManager)
        registerEditPersistent(commandManager)
    }

    private fun <C : CloudSender> registerEditGroup(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("edit")
                .literal("group")
                .required("group", stringParser()) { _, _ ->
                    api.group().allGroups.thenApply { groups ->
                        groups.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .required("setting", stringParser()) { _, _ ->
                    CompletableFuture.completedFuture(groupSettings.map { Suggestion.suggestion(it) })
                }
                .required("value", stringParser())
                .handler { context ->
                    val groupName = context.get<String>("group")
                    val setting = context.get<String>("setting")
                    val value = context.get<String>("value")
                    scope.launch {
                        try {
                            val group = api.group().getGroupByName(groupName).await()
                            val request = UpdateGroupRequest()
                            request.setName(group.name)
                            request.setType(group.type)
                            request.setMinMemory(group.minMemory)
                            request.setMaxMemory(group.maxMemory)
                            request.setMaxPlayers(group.maxPlayers)
                            group.deployment?.let { request.setDeployment(copyDeploymentConfig(it)) }
                            group.scaling?.let { request.setScaling(copyScalingConfig(it)) }
                            group.source?.let { request.setSource(copySourceConfig(it)) }
                            group.workflows?.let { request.setWorkflows(copyWorkflowsConfig(it)) }
                            request.setProperties(group.properties)
                            request.setTags(group.tags)
                            when (setting) {
                                "max-players" -> request.setMaxPlayers(parseIntOrError(value, setting, context.sender()) ?: return@launch)
                                "min-memory" -> request.setMinMemory(parseIntOrError(value, setting, context.sender()) ?: return@launch)
                                "max-memory" -> request.setMaxMemory(parseIntOrError(value, setting, context.sender()) ?: return@launch)
                                else -> {
                                    context.sender().sendMessage(miniMessage(plugin.messageConfiguration.invalidSetting, Placeholder.unparsed("key", setting)))
                                    return@launch
                                }
                            }
                            val updated = api.group().updateGroup(group.serverGroupId, request).await()
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.groupUpdated, GroupTagResolver.of(updated)))
                        } catch (e: Exception) {
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.errorMessage, Placeholder.unparsed("error", e.message ?: "Unknown error")))
                        }
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.edit.group"))
                .build()
        )
    }

    private fun copyDeploymentConfig(original: DeploymentConfig): DeploymentConfig {
        val copy = DeploymentConfig()
        copy.strategy = original.strategy
        original.hosts?.let { hosts ->
            copy.hosts = hosts.map { host ->
                DeploymentHost().apply {
                    name = host.name
                    priority = host.priority
                }
            }.toTypedArray()
        }
        return copy
    }

    private fun copyScalingConfig(original: ScalingConfig): ScalingConfig {
        val copy = ScalingConfig()
        copy.availableSlots = original.availableSlots
        copy.maxServers = original.maxServers
        copy.minServers = original.minServers
        copy.playerThreshold = original.playerThreshold
        copy.scalingMode = original.scalingMode
        original.scaleDown?.let {
            val scaleDownCopy = ScaleDownConfig()
            scaleDownCopy.idleTime = it.idleTime
            scaleDownCopy.isIgnorePlayers = it.isIgnorePlayers
            copy.scaleDown = scaleDownCopy
        }
        return copy
    }

    private fun copySourceConfig(original: SourceConfig): SourceConfig {
        val copy = SourceConfig()
        copy.type = original.type
        copy.blueprint = original.blueprint
        copy.image = original.image
        return copy
    }

    private fun copyWorkflowsConfig(original: WorkflowsConfig): WorkflowsConfig {
        val copy = WorkflowsConfig()
        copy.manual = original.manual
        original.`when`?.let { whenConfig ->
            val whenCopy = WorkflowWhen()
            whenCopy.start = whenConfig.start
            whenCopy.stop = whenConfig.stop
            copy.`when` = whenCopy
        }
        return copy
    }

    private fun <C : CloudSender> registerEditServer(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("edit")
                .literal("server")
                .required("group", stringParser()) { _, _ ->
                    api.group().allGroups.thenApply { groups ->
                        groups.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .required("id", integerParser()) { context, _ ->
                    try {
                        val group = context.get<String>("group")
                        api.server().getServersByGroup(group).thenApply { servers ->
                            servers.map { Suggestion.suggestion(it.numericalId.toString()) }
                        }.exceptionally { emptyList() }
                    } catch (e: Exception) {
                        CompletableFuture.completedFuture(emptyList())
                    }
                }
                .required("setting", stringParser()) { _, _ ->
                    CompletableFuture.completedFuture(serverSettings.map { Suggestion.suggestion(it) })
                }
                .required("value", stringParser())
                .handler { context ->
                    val groupName = context.get<String>("group")
                    val id = context.get<Int>("id")
                    val setting = context.get<String>("setting")
                    val value = context.get<String>("value")
                    scope.launch {
                        try {
                            val server = api.server().getServerByNumericalId(groupName, id).await()
                            val request = UpdateServerRequest().apply {
                                when (setting) {
                                    "max-players" -> maxPlayers = parseIntOrError(value, setting, context.sender()) ?: return@launch
                                    "min-memory" -> minMemory = parseIntOrError(value, setting, context.sender()) ?: return@launch
                                    "max-memory" -> maxMemory = parseIntOrError(value, setting, context.sender()) ?: return@launch
                                    else -> {
                                        context.sender().sendMessage(miniMessage(plugin.messageConfiguration.invalidSetting, Placeholder.unparsed("key", setting)))
                                        return@launch
                                    }
                                }
                            }
                            val updated = api.server().updateServer(server.serverId, request).await()
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.serverUpdated, ServerTagResolver.of(updated)))
                        } catch (e: Exception) {
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.errorMessage, Placeholder.unparsed("error", e.message ?: "Unknown error")))
                        }
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.edit.server"))
                .build()
        )
    }

    private fun <C : CloudSender> registerEditPersistent(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("edit")
                .literal("persistent")
                .required("server", stringParser()) { _, _ ->
                    api.persistentServer().allPersistentServers.thenApply { servers ->
                        servers.map { Suggestion.suggestion(it.name) }
                    }.exceptionally { emptyList() }
                }
                .required("setting", stringParser()) { _, _ ->
                    CompletableFuture.completedFuture(persistentSettings.map { Suggestion.suggestion(it) })
                }
                .required("value", stringParser())
                .handler { context ->
                    val serverName = context.get<String>("server")
                    val setting = context.get<String>("setting")
                    val value = context.get<String>("value")
                    scope.launch {
                        try {
                            val persistent = api.persistentServer().getPersistentServerByName(serverName).await()
                            val builder = persistentUpdateBuilder(persistent)
                            when (setting) {
                                "max-players" -> builder.maxPlayers(parseIntOrError(value, setting, context.sender()) ?: return@launch)
                                "min-memory" -> builder.minMemory(parseIntOrError(value, setting, context.sender()) ?: return@launch)
                                "max-memory" -> builder.maxMemory(parseIntOrError(value, setting, context.sender()) ?: return@launch)
                                "active" -> builder.active(parseBoolOrError(value, setting, context.sender()) ?: return@launch)
                                else -> {
                                    context.sender().sendMessage(miniMessage(plugin.messageConfiguration.invalidSetting, Placeholder.unparsed("key", setting)))
                                    return@launch
                                }
                            }
                            val updated = api.persistentServer().updatePersistentServer(
                                persistent.persistentServerId, builder.build()
                            ).await()
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.persistentServerUpdated, PersistentServerTagResolver.of(updated)))
                        } catch (e: Exception) {
                            context.sender().sendMessage(miniMessage(plugin.messageConfiguration.errorMessage, Placeholder.unparsed("error", e.message ?: "Unknown error")))
                        }
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.edit.persistent"))
                .build()
        )
    }

    private fun parseIntOrError(value: String, key: String, sender: CloudSender): Int? {
        return value.toIntOrNull() ?: run {
            sender.sendMessage(miniMessage(plugin.messageConfiguration.invalidValue, Placeholder.unparsed("value", value), Placeholder.unparsed("key", key)))
            null
        }
    }

    private fun parseBoolOrError(value: String, key: String, sender: CloudSender): Boolean? {
        return value.toBooleanStrictOrNull() ?: run {
            sender.sendMessage(miniMessage(plugin.messageConfiguration.invalidValue, Placeholder.unparsed("value", value), Placeholder.unparsed("key", key)))
            null
        }
    }

    companion object {
        fun persistentUpdateBuilder(persistent: PersistentServer): UpdatePersistentServerRequest.Builder {
            return UpdatePersistentServerRequest.builder()
                .name(persistent.name)
                .type(persistent.type)
                .source(persistent.source)
                .workflows(persistent.workflows)
                .serverhostId(persistent.serverhostId)
                .minMemory(persistent.minMemory)
                .maxMemory(persistent.maxMemory)
                .maxPlayers(persistent.maxPlayers)
                .active(persistent.isActive)
                .properties(persistent.properties)
                .tags(persistent.tags)
        }
    }
}
