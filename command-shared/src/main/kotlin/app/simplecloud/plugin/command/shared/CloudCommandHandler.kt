package app.simplecloud.plugin.command.shared

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.group.Group
import build.buf.gen.simplecloud.controller.v1.ServerStopCause
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.LongParser.longParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.permission.Permission
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import java.util.concurrent.CompletableFuture

// TODO: maybe move this into multiple classes?
class CloudCommandHandler<C : CloudSender>(
    private val commandManager: CommandManager<C>,
    val commandPlugin: CommandPlugin
) {

    private val controllerApi = ControllerApi.createFutureApi()

    fun createCloudCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .handler { context: CommandContext<C> ->

                    context.sender().sendMessage(
                        MiniMessage.miniMessage().deserialize(commandPlugin.messageConfiguration.cloudHelpTitle)
                    )
                    context.sender().sendMessage(
                        MiniMessage.miniMessage().deserialize(commandPlugin.messageConfiguration.cloudStartCommand)
                    )
                    context.sender().sendMessage(
                        MiniMessage.miniMessage().deserialize(commandPlugin.messageConfiguration.cloudStopCommand)
                    )
                    context.sender().sendMessage(
                        MiniMessage.miniMessage().deserialize(commandPlugin.messageConfiguration.cloudServerInfoCommand)
                    )
                    context.sender().sendMessage(
                        MiniMessage.miniMessage().deserialize(commandPlugin.messageConfiguration.cloudGroupInfoCommand)
                    )
                    context.sender().sendMessage(
                        MiniMessage.miniMessage().deserialize(commandPlugin.messageConfiguration.cloudEditGroupCommand)
                    )
                    context.sender().sendMessage(
                        MiniMessage.miniMessage().deserialize(commandPlugin.messageConfiguration.cloudEditServerCommand)
                    )
                    context.sender().sendMessage(
                        MiniMessage.miniMessage()
                            .deserialize(commandPlugin.messageConfiguration.cloudDeleteGroupCommand)
                    )
                }
                .permission(Permission.permission("simplecloud.command.cloud"))
                .build()
        )

        registerStartCommand()
        registerStopCommand()
        registerServerInfoCommand()
        registerGroupInfoCommand()
        registerDeleteGroupCommand()
        registerEditGroupCommand()
        registerEditServerCommand()
    }

    private fun registerStartCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("start")
                .required(
                    "group",
                    stringParser(),
                    SuggestionProvider { _, _ ->
                        controllerApi.getGroups().getAllGroups().thenApply { groups ->
                            groups.map { group -> Suggestion.suggestion(group.name) }
                        }
                    }
                )
                .handler { context: CommandContext<C> ->
                    val group = context.get<String>("group")

                    controllerApi.getServers().startServer(group).thenAccept { server ->
                        val message = MiniMessage.miniMessage().deserialize(
                            commandPlugin.messageConfiguration.serverStarting,
                            Placeholder.component("group", Component.text(group)),
                            Placeholder.component("id", Component.text(server?.numericalId!!))
                        )

                        context.sender().sendMessage(message)
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.start"))
                .build()
        )
    }

    private fun registerStopCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("stop")
                .required("group", stringParser(), SuggestionProvider { _, _ ->
                    controllerApi.getGroups().getAllGroups().thenApply { groups ->
                        groups.map { group -> Suggestion.suggestion(group.name) }
                    }
                })
                .optional("id", longParser(), SuggestionProvider { _, _ ->
                    controllerApi.getServers().getAllServers().thenApply { servers ->
                        servers.map { server -> Suggestion.suggestion(server.numericalId.toString()) }
                    }
                })
                .handler { context: CommandContext<C> ->
                    val group = context.get<String>("group")
                    val id = context.getOrDefault("id", null as Long?)

                    if (id == null) {
                        val message = MiniMessage.miniMessage().deserialize(
                            commandPlugin.messageConfiguration.groupServerStopped,
                            Placeholder.component("group", Component.text(group)),
                        )

                        controllerApi.getServers().getServersByGroup(group).thenAccept { servers ->
                            servers.forEach { server ->
                                controllerApi.getServers().stopServer(
                                    server.group,
                                    server.numericalId.toLong()
                                )
                            }
                        }

                        context.sender().sendMessage(message)
                    } else {

                        val message = MiniMessage.miniMessage().deserialize(
                            commandPlugin.messageConfiguration.serverStopped,
                            Placeholder.component("group", Component.text(group)),
                            Placeholder.component("id", Component.text(id.toString()))
                        )

                        controllerApi.getServers().stopServer(group, id)
                        context.sender().sendMessage(message)
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.stop"))
                .build()
        )
    }

    private fun registerServerInfoCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("info", "get")
                .literal("servers", "server")
                .optional("group", stringParser(), SuggestionProvider { _, _ ->
                    controllerApi.getGroups().getAllGroups().thenApply { groups ->
                        groups.map { group -> Suggestion.suggestion(group.name) }
                    }
                })
                .optional("id", longParser(), SuggestionProvider { _, _ ->
                    controllerApi.getServers().getAllServers().thenApply { servers ->
                        servers.map { server -> Suggestion.suggestion(server.numericalId.toString()) }
                    }
                })
                .handler { context: CommandContext<C> ->
                    val groupName = context.getOrDefault("group", null as String?)
                    val id = context.getOrDefault("id", null as Long?)

                    when {
                        groupName != null && id != null -> {
                            controllerApi.getServers().getServerByNumerical(groupName, id).thenAccept { server ->
                                controllerApi.getGroups().getGroupByName(groupName).thenAccept { group ->
                                    context.sender().sendMessage(
                                        MiniMessage.miniMessage()
                                            .deserialize(
                                                commandPlugin.messageConfiguration.serverInfoTitle,
                                                Placeholder.component("servergroup", Component.text(server.group)),
                                                Placeholder.component(
                                                    "serveramount",
                                                    Component.text(
                                                        controllerApi.getServers().getServersByGroup(groupName)
                                                            .get().size.toString()
                                                    )
                                                ),
                                            )
                                    )
                                    context.sender().sendMessage(
                                        MiniMessage.miniMessage()
                                            .deserialize(
                                                commandPlugin.messageConfiguration.serverInfoType,
                                                Placeholder.component("grouptype", Component.text(server.type.name))
                                            )
                                    )
                                    context.sender().sendMessage(
                                        MiniMessage.miniMessage()
                                            .deserialize(
                                                commandPlugin.messageConfiguration.serverInfoSoftware,
                                                Placeholder.component(
                                                    "groupsoftware",
                                                    Component.text(group.properties["server-software"].toString())
                                                )
                                            )
                                    )
                                    context.sender().sendMessage(
                                        MiniMessage.miniMessage()
                                            .deserialize(
                                                commandPlugin.messageConfiguration.serverInfoMemory,
                                                Placeholder.component("groupmemory", Component.text(server.maxMemory))
                                            )
                                    )
                                    context.sender().sendMessage(
                                        MiniMessage.miniMessage()
                                            .deserialize(
                                                commandPlugin.messageConfiguration.serverInfoPlayers,
                                                Placeholder.component(
                                                    "groupplayers",
                                                    Component.text(server.playerCount)
                                                )
                                            )
                                    )
                                }
                            }
                        }

                        groupName != null -> {
                            controllerApi.getServers().getServersByGroup(groupName).thenAccept { servers ->
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.groupServerListTitle,
                                        Placeholder.component("servergroup", Component.text(groupName))
                                    )
                                )
                                servers.forEach { server ->
                                    context.sender().sendMessage(
                                        MiniMessage.miniMessage().deserialize(
                                            commandPlugin.messageConfiguration.groupServerListEntry,
                                            Placeholder.component("servergroup", Component.text(server.group)),
                                            Placeholder.component(
                                                "numericalid",
                                                Component.text(server.numericalId.toString())
                                            ),
                                            Placeholder.component(
                                                "onlineplayers",
                                                Component.text(server.playerCount)
                                            ),
                                            Placeholder.component("maxplayers", Component.text(server.maxPlayers)),
                                            Placeholder.component("minmemory", Component.text(server.minMemory)),
                                            Placeholder.component("maxmemory", Component.text(server.maxMemory)),
                                            Placeholder.component("state", Component.text(server.state.name)),
                                        )
                                    )
                                }
                            }
                        }

                        else -> {
                            controllerApi.getServers().getAllServers().thenAccept { servers ->
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.serverListTitle,
                                    )
                                )
                                servers.forEach { server ->
                                    context.sender().sendMessage(
                                        MiniMessage.miniMessage().deserialize(
                                            commandPlugin.messageConfiguration.serverListEntry,
                                            Placeholder.component("servergroup", Component.text(server.group)),
                                            Placeholder.component(
                                                "numericalid",
                                                Component.text(server.numericalId.toString())
                                            ),
                                            Placeholder.component(
                                                "onlineplayers",
                                                Component.text(server.playerCount)
                                            ),
                                            Placeholder.component("maxplayers", Component.text(server.maxPlayers)),
                                            Placeholder.component("minmemory", Component.text(server.minMemory)),
                                            Placeholder.component("maxmemory", Component.text(server.maxMemory)),
                                            Placeholder.component("state", Component.text(server.state.name)),
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.get.servers"))
                .build()
        )
    }

    private fun registerGroupInfoCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("info", "get")
                .literal("groups", "group")
                .optional("group", stringParser(), SuggestionProvider { _, _ ->
                    controllerApi.getGroups().getAllGroups().thenApply { groups ->
                        groups.map { group -> Suggestion.suggestion(group.name) }
                    }
                })
                .handler { context: CommandContext<C> ->
                    val groupName = context.getOrDefault("group", null as String?)
                    if (groupName != null) {
                        controllerApi.getGroups().getGroupByName(groupName).thenAccept { group ->
                            controllerApi.getServers().getServersByGroup(groupName).thenAccept { servers ->
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.groupInfoTitle,
                                        Placeholder.component("servergroup", Component.text(groupName)),
                                        Placeholder.component("serveramount", Component.text(servers.size))
                                    )
                                )

                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.groupInfoType,
                                        Placeholder.component("grouptype", Component.text(group.type.name))
                                    )
                                )

                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.groupInfoTemplate,
                                        Placeholder.component(
                                            "grouptemplate",
                                            Component.text(group.properties.get("template-id").toString())
                                        )
                                    )
                                )

                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.groupInfoMemory,
                                        Placeholder.component("minmemory", Component.text(group.minMemory)),
                                        Placeholder.component("maxmemory", Component.text(group.maxMemory))
                                    )
                                )

                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.groupInfoPlayers,
                                        Placeholder.component("maxplayers", Component.text(group.maxPlayers))
                                    )
                                )
                            }
                        }
                    } else {
                        controllerApi.getGroups().getAllGroups().thenAccept { groups ->
                            context.sender().sendMessage(
                                MiniMessage.miniMessage()
                                    .deserialize(commandPlugin.messageConfiguration.groupsListTitle)
                            )
                            groups.forEach { group ->
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.groupsListEntry,
                                        Placeholder.component("servergroup", Component.text(group.name)),
                                        Placeholder.component(
                                            "onlinecount",
                                            Component.text(
                                                controllerApi.getServers().getServersByGroup(group)
                                                    .get().size.toString()
                                            )
                                        ),
                                        Placeholder.component(
                                            "template",
                                            Component.text(group.properties["template-id"].toString())
                                        ),
                                        Placeholder.component("type", Component.text(group.type.name)),
                                        Placeholder.component("maxcount", Component.text(group.maxOnlineCount)),
                                        Placeholder.component("minmemory", Component.text(group.minMemory)),
                                        Placeholder.component("maxmemory", Component.text(group.maxMemory)),
                                    )
                                )

                            }
                        }
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.get.groups"))
                .build()
        )
    }

    private fun registerDeleteGroupCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("delete")
                .literal("group")
                .required("group", stringParser(), SuggestionProvider { _, _ ->
                    controllerApi.getGroups().getAllGroups().thenApply { groups ->
                        groups.map { group -> Suggestion.suggestion(group.name) }
                    }
                })
                .handler { context: CommandContext<C> ->
                    val group = context.get<String>("group")

                    val message = MiniMessage.miniMessage().deserialize(
                        commandPlugin.messageConfiguration.groupDeleted,
                        Placeholder.component("group", Component.text(group))
                    )

                    controllerApi.getServers().getServersByGroup(group).thenAccept { servers ->
                        servers.forEach { server ->
                            controllerApi.getServers().stopServer(
                                server.group,
                                server.numericalId.toLong()
                            )
                        }
                    }

                    controllerApi.getGroups().deleteGroup(group)
                    context.sender().sendMessage(message)
                }
                .permission(Permission.permission("simplecloud.command.cloud.delete.group"))
                .build()
        )
    }

    private fun registerEditGroupCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("edit")
                .literal("group")
                .required("group", stringParser(), SuggestionProvider { _, _ ->
                    controllerApi.getGroups().getAllGroups().thenApply { groups ->
                        groups.map { group -> Suggestion.suggestion(group.name) }
                    }
                })
                .required("setting", stringParser(), SuggestionProvider { _, _ ->
                    CompletableFuture.completedFuture(
                        listOf(
                            "max-players",
                            "max-memory",
                            "max-online-count",
                            "min-memory",
                            "min-online-count",
                            "properties",
                            "server-url",
                            "start-port"
                        ).map { Suggestion.suggestion(it) }
                    )
                })
                .required("value", stringParser())
                .handler { context: CommandContext<C> ->
                    val groupName = context.get<String>("group")
                    val setting = context.get<String>("setting")
                    val value = context.get<String>("value")

                    controllerApi.getGroups().getGroupByName(groupName).thenAccept { group ->
                        val updatedGroup = when (setting) {
                            "max-players" -> group.copy(
                                maxPlayers = value.toLongOrNull() ?: return@thenAccept context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.invalidValue,
                                        Placeholder.component("value", Component.text(value)),
                                        Placeholder.component("key", Component.text(setting))
                                    )
                                )
                            )

                            "max-memory" -> group.copy(
                                maxMemory = value.toLongOrNull() ?: return@thenAccept context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.invalidValue,
                                        Placeholder.component("value", Component.text(value)),
                                        Placeholder.component("key", Component.text(setting))
                                    )
                                )
                            )

                            "min-memory" -> group.copy(
                                minMemory = value.toLongOrNull() ?: return@thenAccept context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.invalidValue,
                                        Placeholder.component("value", Component.text(value)),
                                        Placeholder.component("key", Component.text(setting))
                                    )
                                )
                            )

                            "max-online-count" -> group.copy(
                                maxOnlineCount = value.toLongOrNull() ?: return@thenAccept context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.invalidValue,
                                        Placeholder.component("value", Component.text(value)),
                                        Placeholder.component("key", Component.text(setting))
                                    )
                                )
                            )

                            "min-online-count" -> group.copy(
                                minOnlineCount = value.toLongOrNull() ?: return@thenAccept context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.invalidValue,
                                        Placeholder.component("value", Component.text(value)),
                                        Placeholder.component("key", Component.text(setting))
                                    )
                                )
                            )

                            "start-port" -> group.copy(
                                startPort = value.toLongOrNull() ?: return@thenAccept context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.invalidValue,
                                        Placeholder.component("value", Component.text(value)),
                                        Placeholder.component("key", Component.text(setting))
                                    )
                                )
                            )

                            "server-url" -> group.copy(properties = group.properties + mapOf("server-url" to value))
                            "properties" -> group.copy(properties = group.properties + mapOf("custom-property" to value))
                            else -> {
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.invalidSetting,
                                        Placeholder.component("key", Component.text(setting))
                                    )
                                )
                                return@thenAccept
                            }
                        }

                        controllerApi.getGroups().updateGroup(updatedGroup).thenAccept { group ->
                            context.sender().sendMessage(
                                MiniMessage.miniMessage().deserialize(
                                    commandPlugin.messageConfiguration.groupUpdated,
                                    Placeholder.component("group", Component.text(group.name))
                                )
                            )
                        }
                    }


                }
                .permission(Permission.permission("simplecloud.command.cloud.edit.group"))
                .build()
        )
    }

    private fun registerEditServerCommand() {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("edit")
                .literal("server")
                .required("group", stringParser(), SuggestionProvider { _, _ ->
                    controllerApi.getGroups().getAllGroups().thenApply { groups ->
                        groups.map { group -> Suggestion.suggestion(group.name) }
                    }
                })
                .required("id", longParser(), SuggestionProvider { _, _ ->
                    controllerApi.getServers().getAllServers().thenApply { servers ->
                        servers.map { server -> Suggestion.suggestion(server.numericalId.toString()) }
                    }
                })
                .required("setting", stringParser(), SuggestionProvider { _, _ ->
                    CompletableFuture.completedFuture(
                        listOf("max-players").map { Suggestion.suggestion(it) }
                    )
                })
                .required("value", stringParser())
                .handler { context: CommandContext<C> ->
                    val groupName = context.get<String>("group")
                    val serverId = context.get<Long>("id")
                    val setting = context.get<String>("setting")
                    val value = context.get<String>("value")

                    controllerApi.getServers().getServerByNumerical(groupName, serverId).thenAccept { server ->
                        val updatedServer = when (setting) {
                            "max-players" -> server.copy(
                                maxPlayers = value.toLongOrNull() ?: return@thenAccept context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.invalidValue,
                                        Placeholder.component("value", Component.text(value)),
                                        Placeholder.component("key", Component.text(setting))
                                    )
                                )
                            )

                            else -> {
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.invalidSetting,
                                        Placeholder.component("key", Component.text(setting))
                                    )
                                )
                                return@thenAccept
                            }
                        }

                        controllerApi.getServers().updateServer(updatedServer).thenAccept { server ->
                            context.sender().sendMessage(
                                MiniMessage.miniMessage().deserialize(
                                    commandPlugin.messageConfiguration.serverUpdated,
                                    Placeholder.component("group", Component.text(server.group)),
                                    Placeholder.component("numericalid", Component.text(server.numericalId.toString()))
                                )
                            )
                        }
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.edit.server"))
                .build()
        )
    }
}
