package app.simplecloud.plugin.command.shared.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.api.server.StartServerRequest
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

class StartCommand<C : CloudSender>(
    private val cloudApi: CloudApi,
    private val commandPlugin: CommandPlugin
) {

    fun register(commandManager: CommandManager<C>) {
        commandManager.command(
            commandManager.commandBuilder("cloud")
                .literal("start")
                .required(
                    "group",
                    stringParser(),
                    SuggestionProvider { _, _ ->
                        cloudApi.group().allGroups.thenApply { groups ->
                            groups.map { group -> Suggestion.suggestion(group.name) }
                        }
                    }
                )
                .handler { context: CommandContext<C> ->
                    val group = context.get<String>("group")

                    cloudApi.group().getGroupByName(group).thenAccept { group ->
                        val request = StartServerRequest(group.serverGroupId, group.name)

                        cloudApi.server().startServer(request).thenAccept { server ->
                            val message = MiniMessage.miniMessage().deserialize(
                                commandPlugin.messageConfiguration.serverStarting,
                                Placeholder.component("group", Component.text(group.name)),
                                // TODO: server returns a Void
                                Placeholder.component("id", Component.text(server))
                            )

                            context.sender().sendMessage(message)
                        }
                    }

                }
                .permission(Permission.permission("simplecloud.command.cloud.start"))
                .build()
        )
    }
}

