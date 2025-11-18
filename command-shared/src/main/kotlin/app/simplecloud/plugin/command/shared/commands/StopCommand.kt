package app.simplecloud.plugin.command.shared.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.api.server.ServerQuery
import app.simplecloud.plugin.command.shared.CloudSender
import app.simplecloud.plugin.command.shared.CommandPlugin
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

class StopCommand<C : CloudSender>(
    private val cloudApi: CloudApi,
    private val commandPlugin: CommandPlugin
) {

    fun register(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("stop")
                .required("group", stringParser(), SuggestionProvider { _, _ ->
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
                    val group = context.get<String>("group")
                    val id = context.getOrDefault("id", null as Long?)

                    if (id == null) {
                        val message = MiniMessage.miniMessage().deserialize(
                            commandPlugin.messageConfiguration.groupServerStopped,
                            Placeholder.component("group", Component.text(group)),
                        )

                        // TODO: get server by group

                        cloudApi.server().getAllServers(ServerQuery.create().filterByServerGroupName(group))
                            .thenApply { servers ->
                                servers.forEach { server ->
                                    cloudApi.server().stopServer(server.serverId)
                                }
                            }

                        context.sender().sendMessage(message)
                    } else {
                        val message = MiniMessage.miniMessage().deserialize(
                            commandPlugin.messageConfiguration.serverStopped,
                            Placeholder.component("group", Component.text(group)),
                            Placeholder.component("id", Component.text(id.toString()))
                        )

                        // TODO: stop server by numerical

                        // controllerApi.getServers().stopServer(group, id)
                        context.sender().sendMessage(message)
                    }
                }
                .permission(Permission.permission("simplecloud.command.cloud.stop"))
                .build()
        )
    }
}

