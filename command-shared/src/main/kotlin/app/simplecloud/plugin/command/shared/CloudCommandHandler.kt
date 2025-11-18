package app.simplecloud.plugin.command.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.command.shared.commands.DeleteGroupCommand
import app.simplecloud.plugin.command.shared.commands.GroupInfoCommand
import app.simplecloud.plugin.command.shared.commands.ServerInfoCommand
import app.simplecloud.plugin.command.shared.commands.StartCommand
import app.simplecloud.plugin.command.shared.commands.StopCommand
import net.kyori.adventure.text.minimessage.MiniMessage
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.permission.Permission

class CloudCommandHandler<C : CloudSender>(
    private val commandManager: CommandManager<C>,
    val commandPlugin: CommandPlugin
) {

    private val cloudApi = CloudApi.create()

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

        StartCommand(cloudApi, commandPlugin).register(commandManager)
        StopCommand(cloudApi, commandPlugin).register(commandManager)
        ServerInfoCommand(cloudApi, commandPlugin).register(commandManager)
        GroupInfoCommand(cloudApi, commandPlugin).register(commandManager)
        DeleteGroupCommand(cloudApi, commandPlugin).register(commandManager)
    }
}
