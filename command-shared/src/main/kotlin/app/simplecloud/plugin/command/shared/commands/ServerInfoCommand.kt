package app.simplecloud.plugin.command.shared.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.command.shared.CloudSender
import app.simplecloud.plugin.command.shared.CommandPlugin
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.LongParser.longParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.permission.Permission
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import kotlin.text.get

class ServerInfoCommand<C : CloudSender>(
    private val cloudApi: CloudApi,
    private val commandPlugin: CommandPlugin
) {

    fun register(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("info", "get")
                .literal("servers", "server")
                .optional("group", stringParser(), SuggestionProvider { _, _ ->
                    cloudApi.group().allGroups.thenApply { groups ->
                        groups.map { group -> Suggestion.suggestion(group.name) }
                    }
                })
                .optional("id", longParser(), SuggestionProvider { _, _ ->
                    cloudApi.server().allServers.thenApply { servers ->
                        servers.map { server -> Suggestion.suggestion(server.numericalId.toString()) }
                    }
                })
                .handler { context: CommandContext<C> ->
                    val groupName = context.getOrDefault("group", null as String?)
                    val id = context.getOrDefault("id", null as Long?)

                    when {
                        groupName != null && id != null -> {
                            /* TODO: get server by numerical
                            controllerApi.getServers().getServerByNumerical(groupName, id).thenAccept { server ->

                            cloudApi.group().getGroupByName(groupName).thenAccept { group ->
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
                     }   */
                }
                .permission(Permission.permission("simplecloud.command.cloud.get.servers"))
                .build()
        )
    }
}

