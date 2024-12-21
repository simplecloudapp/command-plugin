package app.simplecloud.plugin.command.shared

import app.simplecloud.controller.api.ControllerApi
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

                    controllerApi.getServers().startServer(group).thenApply { server ->
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
                .required("id", longParser(), SuggestionProvider { _, _ ->
                    controllerApi.getServers().getAllServers().thenApply { servers ->
                        servers.map { server -> Suggestion.suggestion(server.numericalId.toString()) }
                    }
                })
                .handler { context: CommandContext<C> ->
                    val group = context.get<String>("group")
                    val id = context.get<Long>("id")

                    val message = MiniMessage.miniMessage().deserialize(
                        commandPlugin.messageConfiguration.serverStopped,
                        Placeholder.component("group", Component.text(group)),
                        Placeholder.component("id", Component.text(id.toString()))
                    )

                    context.sender().sendMessage(message)
                    controllerApi.getServers().stopServer(group, id)
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
                            controllerApi.getServers().getServerByNumerical(groupName, id).thenApply { server ->
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage()
                                        .deserialize(
                                            commandPlugin.messageConfiguration.serverInfoTitle,
                                            Placeholder.component("serverGroup", Component.text(server.group)),
                                            // TODO: real serverAmount
                                            Placeholder.component("serverAmount", Component.text("1")),
                                        )
                                )
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage()
                                        .deserialize(
                                            commandPlugin.messageConfiguration.serverInfoType,
                                            Placeholder.component("groupType", Component.text(server.type.name))
                                        )
                                )
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage()
                                        .deserialize(
                                            commandPlugin.messageConfiguration.serverInfoSoftware,
                                            // TODO: real software
                                            Placeholder.component("groupSoftware", Component.text("PAPER"))
                                        )
                                )
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage()
                                        .deserialize(
                                            commandPlugin.messageConfiguration.serverInfoMemory,
                                            Placeholder.component("groupMemory", Component.text(server.maxMemory))
                                        )
                                )
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage()
                                        .deserialize(
                                            commandPlugin.messageConfiguration.serverInfoPlayers,
                                            Placeholder.component("groupPlayers", Component.text(server.playerCount))
                                        )
                                )
                            }
                        }

                        groupName != null -> {
                            controllerApi.getServers().getServersByGroup(groupName).thenApply { servers ->
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.groupServerListTitle,
                                        Placeholder.component("serverGroup", Component.text(groupName))
                                    )
                                )
                                servers.forEach { server ->
                                    context.sender().sendMessage(
                                        MiniMessage.miniMessage().deserialize(
                                            commandPlugin.messageConfiguration.groupServerListEntry,
                                            Placeholder.component("serverGroup", Component.text(server.group)),
                                            Placeholder.component(
                                                "numericalId",
                                                Component.text(server.numericalId.toString())
                                            ),
                                            Placeholder.component("onlinePlayers", Component.text(server.playerCount)),
                                            Placeholder.component("maxPlayers", Component.text(server.maxPlayers)),
                                            Placeholder.component("minMemory", Component.text(server.minMemory)),
                                            Placeholder.component("maxMemory", Component.text(server.maxMemory)),
                                            Placeholder.component("state", Component.text(server.state.name)),
                                        )
                                    )
                                }
                            }
                        }

                        id != null -> {
                            // TODO
                            println("Getting server with ID $id")
                        }

                        else -> {
                            controllerApi.getServers().getAllServers().thenApply { servers ->
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.serverListTitle,
                                    )
                                )
                                servers.forEach { server ->
                                    context.sender().sendMessage(
                                        MiniMessage.miniMessage().deserialize(
                                            commandPlugin.messageConfiguration.serverListEntry,
                                            Placeholder.component("serverGroup", Component.text(server.group)),
                                            Placeholder.component(
                                                "numericalId",
                                                Component.text(server.numericalId.toString())
                                            ),
                                            Placeholder.component("onlinePlayers", Component.text(server.playerCount)),
                                            Placeholder.component("maxPlayers", Component.text(server.maxPlayers)),
                                            Placeholder.component("minMemory", Component.text(server.minMemory)),
                                            Placeholder.component("maxMemory", Component.text(server.maxMemory)),
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
                                        Placeholder.component("serverGroup", Component.text(groupName)),
                                        Placeholder.component("serverAmount", Component.text(servers.size))
                                    )
                                )

                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.groupInfoType,
                                        Placeholder.component("groupType", Component.text(group.type.name))
                                    )
                                )

                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.groupInfoTemplate,
                                        Placeholder.component(
                                            "groupTemplate",
                                            Component.text(group.properties.get("template-id").toString())
                                        )
                                    )
                                )

                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.groupInfoMemory,
                                        Placeholder.component("minMemory", Component.text(group.minMemory)),
                                        Placeholder.component("maxMemory", Component.text(group.maxMemory))
                                    )
                                )

                                context.sender().sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                        commandPlugin.messageConfiguration.groupInfoPlayers,
                                        Placeholder.component("maxPlayers", Component.text(group.maxPlayers))
                                    )
                                )
                            }
                        }
                    } else {
                        controllerApi.getGroups().getAllGroups().thenApply { groups ->
                            context.sender().sendMessage(
                                MiniMessage.miniMessage()
                                    .deserialize(commandPlugin.messageConfiguration.groupsListTitle)
                            )
                            groups.forEach { group ->
                                context.sender().sendMessage(
                                    MiniMessage.miniMessage()
                                        .deserialize(
                                            commandPlugin.messageConfiguration.groupsListEntry,
                                            Placeholder.component("serverGroup", Component.text(group.name)),
                                            // TODO: add online count
                                            Placeholder.component("onlineCount", Component.text("TODO")),
                                            Placeholder.component(
                                                "template",
                                                Component.text(group.properties["template-id"].toString())
                                            ),
                                            Placeholder.component("type", Component.text(group.type.name)),
                                            Placeholder.component("maxCount", Component.text(group.maxOnlineCount)),
                                            Placeholder.component("minMemory", Component.text(group.minMemory)),
                                            Placeholder.component("maxMemory", Component.text(group.maxMemory)),
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

                    controllerApi.getGroups().deleteGroup(group)
                    context.sender().sendMessage(message)
                }
                .permission(Permission.permission("simplecloud.command.cloud.delete.group"))
                .build()
        )
    }
}
