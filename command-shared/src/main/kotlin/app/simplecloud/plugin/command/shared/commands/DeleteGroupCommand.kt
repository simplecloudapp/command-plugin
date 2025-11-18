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

class DeleteGroupCommand<C : CloudSender>(
    private val cloudApi: CloudApi,
    private val commandPlugin: CommandPlugin
) {

    fun register(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("delete")
                .literal("group")
                .required("group", stringParser(), SuggestionProvider { _, _ ->
                    cloudApi.group().allGroups.thenApply { groups ->
                        groups.map { group -> Suggestion.suggestion(group.name) }
                    }
                })
                .handler { context: CommandContext<C> ->
                    val group = context.get<String>("group")

                    val message = MiniMessage.miniMessage().deserialize(
                        commandPlugin.messageConfiguration.groupDeleted,
                        Placeholder.component("group", Component.text(group))
                    )



                    // TODO: getServersByGroup
                    /* controllerApi.getServers().getServersByGroup(group).thenAccept { servers ->
                        servers.forEach { server ->
                            controllerApi.getServers().stopServer(
                                server.group,
                                server.numericalId.toLong()
                            )
                        }
                    }

                    controllerApi.getGroups().deleteGroup(group) */
                    context.sender().sendMessage(message)
                }
                .permission(Permission.permission("simplecloud.command.cloud.delete.group"))
                .build()
        )
    }
}

