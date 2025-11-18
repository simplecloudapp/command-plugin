package app.simplecloud.plugin.command.shared.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.command.shared.CloudSender
import app.simplecloud.plugin.command.shared.CommandPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.permission.Permission
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import kotlin.text.get

class GroupInfoCommand<C : CloudSender>(
    private val cloudApi: CloudApi,
    private val commandPlugin: CommandPlugin
) {

    fun register(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("info", "get")
                .literal("groups", "group")
                .optional("group", stringParser(), SuggestionProvider { _, _ ->
                    cloudApi.group().allGroups.thenApply { groups ->
                        groups.map { group -> Suggestion.suggestion(group.name) }
                    }
                })
                .handler { context: CommandContext<C> ->
                    val groupName = context.getOrDefault("group", null as String?)
                    if (groupName != null) {
                        cloudApi.group().getGroupByName(groupName).thenAccept { group ->
                            // TODO: get server by group
                        }
                        /* controllerApi.getGroups().getGroupByName(groupName).thenAccept { group ->
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
                        }  */
                    } else {
                        cloudApi.group().allGroups.thenAccept { groups ->
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
                                             /* TODO   cloudApi.server().getServersByGroup(group)
                                                    .get().size.toString() */
                                                0
                                            )
                                        ),
                                        Placeholder.component(
                                            "template",
                                            Component.text(group.properties["template-id"].toString())
                                        ),
                                        Placeholder.component("type", Component.text(group.type.name)),
                                        Placeholder.component("maxcount", Component.text(group.maxPlayers)),
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
}

